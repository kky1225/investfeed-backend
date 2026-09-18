package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.CoinDailyFactSheet
import com.example.investfeed.domain.assistant.dto.factsheet.KrCloseFactSheet
import com.example.investfeed.domain.assistant.dto.factsheet.KrHoldingsFactSheet
import com.example.investfeed.domain.assistant.dto.factsheet.KrPreFactSheet
import com.example.investfeed.domain.assistant.dto.factsheet.MarketFactSheet
import com.example.investfeed.domain.assistant.dto.factsheet.PersonalFactSheet
import com.example.investfeed.domain.assistant.dto.factsheet.UsCloseFactSheet
import com.example.investfeed.domain.assistant.dto.message.Headline
import com.example.investfeed.domain.assistant.dto.message.HeadlineScope
import com.example.investfeed.domain.assistant.dto.message.MessageBody
import com.example.investfeed.domain.assistant.dto.message.MessageType
import com.example.investfeed.domain.assistant.entity.AssistantBriefing
import com.example.investfeed.domain.assistant.entity.AssistantBriefingPersonal
import com.example.investfeed.domain.assistant.repository.AssistantBriefingPersonalRepository
import com.example.investfeed.domain.assistant.repository.AssistantBriefingRepository
import com.example.investfeed.domain.auth.entity.Member
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.global.holiday.HolidayService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

@Service
class BriefingService(
    private val marketFactSheetService: MarketFactSheetService,
    private val personalBriefingService: PersonalBriefingService,
    private val usIndexDailyService: UsIndexDailyService,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val holidayService: HolidayService,
    private val briefingRepository: AssistantBriefingRepository,
    private val personalRepository: AssistantBriefingPersonalRepository,
    private val assistantSettingService: AssistantSettingService,
    private val memberRepository: MemberRepository,
    private val timelineService: TimelineService,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    data class RunResult(val type: BriefingType, val targetDate: LocalDate?, val briefingId: Long?, val posted: Int, val skipped: Int)

    fun resolveTargetDate(type: BriefingType, now: LocalDateTime = LocalDateTime.now()): LocalDate? = when (type) {
        BriefingType.KR_CLOSE, BriefingType.KR_HOLDINGS -> holidayService.lastClosedTradingDay(now)
        BriefingType.KR_PRE -> now.toLocalDate().let { if (holidayService.isHoliday(it)) holidayService.nextTradingDay(it) else it }
        BriefingType.US_CLOSE -> usMarketCalendarService.lastClosedUsTradingDay(now)
        BriefingType.COIN_DAILY -> if (now.toLocalTime().isBefore(LocalTime.of(9, 0))) now.toLocalDate().minusDays(2) else now.toLocalDate().minusDays(1)
    }

    fun resolveAsOf(type: BriefingType, date: LocalDate): LocalDateTime = when (type) {
        BriefingType.KR_CLOSE -> date.atTime(16, 0)
        BriefingType.KR_HOLDINGS -> date.atTime(20, 5)
        BriefingType.KR_PRE -> date.atTime(7, 0)
        BriefingType.US_CLOSE -> usMarketCalendarService.closeKst(date).plusMinutes(10)
        BriefingType.COIN_DAILY -> date.plusDays(1).atTime(9, 5)
    }

    fun canGenerate(type: BriefingType, date: LocalDate, now: LocalDateTime): Boolean = when (type) {
        BriefingType.COIN_DAILY -> true
        BriefingType.KR_CLOSE, BriefingType.KR_PRE, BriefingType.KR_HOLDINGS -> now.toLocalDate() == date
        BriefingType.US_CLOSE -> {
            val nextOpenKst = ZonedDateTime.of(usMarketCalendarService.nextUsTradingDay(date), LocalTime.of(9, 30), UsMarketCalendarService.NY)
                .withZoneSameInstant(UsMarketCalendarService.KST).toLocalDateTime()
            now.isBefore(nextOpenKst)
        }
    }

    fun generateAndPublish(type: BriefingType, now: LocalDateTime = LocalDateTime.now()): RunResult {
        val date = resolveTargetDate(type, now) ?: return RunResult(type, null, null, 0, 0)
        val existing = briefingRepository.findFirstByBriefingDateAndTypeOrderByVersionDesc(date, type.subtype)
        if (existing == null && !canGenerate(type, date, now)) {
            log.warn { "브리핑 생성 건너뜀: type=$type date=$date — 세션이 지나 실시간 소스로 재현 불가 (now=$now)" }
            return RunResult(type, date, null, 0, 0)
        }
        val shared = existing ?: generateShared(type, date, resolveAsOf(type, date))
        val (posted, skipped) = publishToMembers(type, date, shared)
        return RunResult(type, date, shared.id, posted, skipped)
    }

    private data class SharedPack(val sheet: MarketFactSheet, val headline: Headline, val rendered: BriefingTemplateRenderer.Rendered)

    private fun buildShared(type: BriefingType, date: LocalDate, asOf: LocalDateTime): SharedPack {
        val sheet = when (type) {
            BriefingType.KR_CLOSE -> marketFactSheetService.collectKrClose(date, asOf)
            BriefingType.KR_PRE -> marketFactSheetService.collectKrPre(date, asOf)
            BriefingType.KR_HOLDINGS -> KrHoldingsFactSheet(date, asOf)
            BriefingType.COIN_DAILY -> marketFactSheetService.collectCoinDaily(date, asOf)
            BriefingType.US_CLOSE -> {
                usIndexDailyService.backfillIfEmpty(date)
                usIndexDailyService.captureFromRedis(date)
                marketFactSheetService.collectUsClose(date, asOf)
            }
        }
        val (headline, rendered) = compose(sheet, null)
        return SharedPack(sheet, headline, rendered)
    }

    private fun compose(sheet: MarketFactSheet, personal: PersonalFactSheet?): Pair<Headline, BriefingTemplateRenderer.Rendered> = when (sheet) {
        is KrCloseFactSheet -> HeadlineSelector.selectKrClose(sheet) to BriefingTemplateRenderer.renderKrClose(sheet)
        is KrPreFactSheet -> HeadlineSelector.selectKrPre(sheet) to BriefingTemplateRenderer.renderKrPre(sheet)
        is UsCloseFactSheet -> HeadlineSelector.selectUsClose(sheet, personal) to BriefingTemplateRenderer.renderUsClose(sheet, personal)
        is CoinDailyFactSheet -> HeadlineSelector.selectCoinDaily(sheet, personal) to BriefingTemplateRenderer.renderCoinDaily(sheet, personal)
        is KrHoldingsFactSheet -> HeadlineSelector.selectKrHoldings(personal) to BriefingTemplateRenderer.renderKrHoldings(personal)
    }

    private fun generateShared(type: BriefingType, date: LocalDate, asOf: LocalDateTime): AssistantBriefing {
        val pack = buildShared(type, date, asOf)
        val body = MessageBody(
            type = MessageType.BRIEFING, subtype = type.subtype, asOf = asOf,
            headline = pack.headline, summary = pack.rendered.summary, sections = pack.rendered.sections,
        )
        val entity = AssistantBriefing(
            briefingDate = date, type = type.subtype, version = 1, asOf = asOf,
            headlineText = pack.headline.text.take(300),
            body = objectMapper.writeValueAsString(body),
            factSheet = objectMapper.writeValueAsString(pack.sheet),
        )
        return try {
            briefingRepository.save(entity)
        } catch (e: DataIntegrityViolationException) {
            briefingRepository.findFirstByBriefingDateAndTypeOrderByVersionDesc(date, type.subtype) ?: throw e
        }
    }

    private fun publishToMembers(type: BriefingType, date: LocalDate, shared: AssistantBriefing): Pair<Int, Int> {
        val sharedBody = objectMapper.readValue(shared.body, MessageBody::class.java)
        val sheet = parseFactSheet(type, shared.factSheet)
        var posted = 0; var skipped = 0
        findRecipients(type).forEach { member ->
            if (timelineService.isPosted(member.id, shared.id)) { skipped++; return@forEach }
            try {
                val personal = buildPersonalSheet(member, type, date)
                val (headline, rendered) = compose(sheet, personal)
                if (rendered.sections.isEmpty()) { skipped++; return@forEach }
                val personalSections = rendered.sections.filter { it.personal }
                val personalEntity = personal?.let {
                    personalRepository.findByBriefingIdAndMemberId(shared.id, member.id) ?: personalRepository.save(
                        AssistantBriefingPersonal(
                            briefingId = shared.id, memberId = member.id,
                            headlineText = headline.text.takeIf { headline.scope == HeadlineScope.PERSONAL }?.take(300),
                            body = objectMapper.writeValueAsString(MessageBody(type = MessageType.BRIEFING, subtype = type.subtype, asOf = shared.asOf, headline = headline, sections = personalSections)),
                            factSheet = objectMapper.writeValueAsString(it),
                        )
                    )
                }
                val off = assistantSettingService.sectionsOff(assistantSettingService.findOrDefault(member.id))
                val sections = rendered.sections.filter { it.id !in off }
                val body = sharedBody.copy(
                    headline = headline,
                    summary = rendered.summary,
                    sections = sections,
                    refs = mapOf("briefingId" to shared.id) + (personalEntity?.let { mapOf("personalBriefingId" to it.id) } ?: emptyMap()),
                )
                timelineService.postMessage(member.id, body, refBriefingId = shared.id, refPersonalBriefingId = personalEntity?.id)
                posted++
            } catch (e: Exception) {
                log.error(e) { "브리핑 회원 게시 실패: type=$type member=${member.loginId}" }
            }
        }
        return posted to skipped
    }

    private fun buildPersonalSheet(member: Member, type: BriefingType, date: LocalDate): PersonalFactSheet? =
        runCatching { personalBriefingService.buildPersonalSheet(member, type, date) }
            .onFailure { log.error(it) { "개인 파트 생성 실패: type=$type member=${member.loginId}" } }
            .getOrNull()

    private fun findRecipients(type: BriefingType): List<Member> =
        memberRepository.findAll().filter { assistantSettingService.enabledFor(assistantSettingService.findOrDefault(it.id), type) }

    private fun parseFactSheet(type: BriefingType, json: String?): MarketFactSheet {
        requireNotNull(json) { "브리핑 팩트시트 없음 (type=$type)" }
        return when (type) {
            BriefingType.KR_CLOSE -> objectMapper.readValue(json, KrCloseFactSheet::class.java)
            BriefingType.KR_PRE -> objectMapper.readValue(json, KrPreFactSheet::class.java)
            BriefingType.US_CLOSE -> objectMapper.readValue(json, UsCloseFactSheet::class.java)
            BriefingType.COIN_DAILY -> objectMapper.readValue(json, CoinDailyFactSheet::class.java)
            BriefingType.KR_HOLDINGS -> objectMapper.readValue(json, KrHoldingsFactSheet::class.java)
        }
    }
}
