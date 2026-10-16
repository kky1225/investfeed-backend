package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.AlertDirection
import com.example.investfeed.domain.assistant.dto.factsheet.AlertTrigger
import com.example.investfeed.domain.assistant.dto.factsheet.IndexAlertCardFact
import com.example.investfeed.domain.assistant.dto.factsheet.IndexAlertFact
import com.example.investfeed.domain.assistant.dto.factsheet.IndexFact
import com.example.investfeed.domain.assistant.dto.factsheet.IndexQuote
import com.example.investfeed.domain.assistant.entity.AssistantIndexAlert
import com.example.investfeed.domain.assistant.entity.AssistantSetting
import com.example.investfeed.domain.assistant.repository.AssistantIndexAlertRepository
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.circuitbreaker.dto.CircuitBreakerFiredEvent
import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.crawler.NaverMarketIndexCrawler
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectPriceNowReq
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

@Service
class IndexAlertService(
    private val sectClient: SectClient,
    private val crawler: NaverMarketIndexCrawler,
    private val alertStateService: AlertStateService,
    private val marketFactSheetService: MarketFactSheetService,
    private val alertRepository: AssistantIndexAlertRepository,
    private val assistantSettingService: AssistantSettingService,
    private val memberRepository: MemberRepository,
    private val timelineService: TimelineService,
    private val telegramNotifier: TelegramNotifier,
    private val objectMapper: ObjectMapper,
    private val usMarketCalendarService: UsMarketCalendarService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        val WARN_LEVELS = listOf(3.0, 5.0)
        val CB_LEVELS_US = listOf(-7.0, -13.0, -20.0)
        private val US_CB_LATE_CUTOFF: java.time.Duration = java.time.Duration.ofMinutes(34)   // 마감 34분 전(15:26·12:26)부터 1·2단계 제외

        const val KOSPI = "KOSPI"; const val KOSDAQ = "KOSDAQ"; const val NASDAQ = "NASDAQ"; const val SP500 = "SP500"
        private val KR_INDS_CD = mapOf(KOSPI to "001", KOSDAQ to "101")
        private val US_TYPES = mapOf(NASDAQ to MarketIndexType.NASDAQ, SP500 to MarketIndexType.SP500)
        private const val RATE_CHECK_TOLERANCE = 0.05
        private const val RANGE_TOLERANCE = 0.0001
        private const val MAX_ABS_RATE = 30.0
    }

    private fun isValidQuote(q: IndexQuote): Boolean {
        if (q.current <= 0 || q.prevClose <= 0 || q.high <= 0 || q.low <= 0) return false   // 체결 전 0 — 정상 상황이라 로그 없음
        if (q.low > q.current * (1 + RANGE_TOLERANCE) || q.high < q.current * (1 - RANGE_TOLERANCE)) {
            log.error { "지수 시세 범위 이상으로 판정 건너뜀: ${q.indexCode} cur=${q.current} high=${q.high} low=${q.low} prev=${q.prevClose}" }
            return false
        }
        if (listOf(q.currentRate, q.highRate, q.lowRate).any { abs(it) > MAX_ABS_RATE }) {
            log.error { "지수 등락률 ±${MAX_ABS_RATE}% 초과로 판정 건너뜀: ${q.indexCode} cur=${q.current} high=${q.high} low=${q.low} prev=${q.prevClose}" }
            return false
        }
        return true
    }

    data class RunResult(val quotes: List<IndexQuote>, val fired: List<IndexAlertFact>, val posted: Int)

    fun runKr(now: LocalDateTime): RunResult {
        val quotes = fetchKrQuotes(now.toLocalDate()).filter { isValidQuote(it) }
        return judgeAndPublish(quotes, now, kr = true)
    }

    fun runUs(now: LocalDateTime, usTradeDate: LocalDate): RunResult {
        val quotes = US_TYPES.mapNotNull { (code, type) ->
            runCatching { crawler.fetchIndexOhlc(type) }
                .onFailure { log.error(it) { "미국 지수 고저가 조회 실패: $code" } }
                .getOrNull()
                ?.takeIf { it.tradeDate == usTradeDate && it.prevClose != null && it.prevClose > 0 }
                ?.let { IndexQuote(code, it.tradeDate, it.close, it.prevClose!!, it.high, it.low) }
                ?.takeIf { isValidQuote(it) }
        }
        return judgeAndPublish(quotes, now, kr = false)
    }

    private fun fetchKrQuotes(date: LocalDate): List<IndexQuote> = runBlocking {
        KR_INDS_CD.map { (code, indsCd) ->
            async {
                runCatching {
                    val res = sectClient.sectPriceNow(KiwoomSectPriceNowReq(mrkt_tp = "0", inds_cd = indsCd))
                    val cur = TemplateFormat.parse(res.cur_prc)?.let { abs(it) } ?: return@runCatching null
                    val high = TemplateFormat.parse(res.high_pric)?.let { abs(it) } ?: return@runCatching null
                    val low = TemplateFormat.parse(res.low_pric)?.let { abs(it) } ?: return@runCatching null
                    val prev = prevClose(cur, TemplateFormat.parse(res.pred_pre), TemplateFormat.parse(res.flu_rt)) ?: return@runCatching null
                    IndexQuote(code, date, cur, prev, high, low)
                }.onFailure { log.error(it) { "국내 지수 현재가 조회 실패: $code" } }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }

    private fun prevClose(cur: Double, predPre: Double?, fluRt: Double?): Double? {
        val byAmount = predPre?.let { cur - it }?.takeIf { it > 0 }
        if (fluRt == null) return byAmount
        if (byAmount != null && abs((cur / byAmount - 1) * 100 - fluRt) <= RATE_CHECK_TOLERANCE) return byAmount
        return (cur / (1 + fluRt / 100)).takeIf { it > 0 }
    }

    private fun judgeAndPublish(quotes: List<IndexQuote>, now: LocalDateTime, kr: Boolean): RunResult {
        val fired = mutableListOf<IndexAlertFact>()
        var posted = 0
        quotes.forEach { q ->
            val fact = judge(q, now, kr) ?: return@forEach
            fired += fact
            posted += runCatching { publish(fact, kr) }
                .onFailure { log.error(it) { "지수 알림 게시 실패: ${q.indexCode} ${fact.trigger}" } }
                .getOrDefault(0)
        }
        return RunResult(quotes, fired, posted)
    }

    private fun judge(q: IndexQuote, now: LocalDateTime, kr: Boolean): IndexAlertFact? {
        val state = alertStateService.get(q.indexCode, q.tradeDate)
        val fired = judgeCb(q, state, now, kr) ?: judgeWarn(q, state, now)
        alertStateService.save(q.indexCode, q.tradeDate, state)
        return fired
    }

    // 국내 CB 는 지수 실시간(0J) 1분 연속 판정(CircuitBreakerService)이 단독 담당 → onCircuitBreakerFired. 여기선 미국(S&P500)만
    private fun judgeCb(q: IndexQuote, state: AlertStateService.IndexAlertState, now: LocalDateTime, kr: Boolean): IndexAlertFact? {
        if (kr || q.indexCode != SP500) return null
        val stage = CB_LEVELS_US.count { q.lowRate <= it }
        if (stage == 0 || stage in state.cbStages) return null
        // 1·2단계는 15:25 ET 까지(포함)만 발동, 15:26 부터는 발동하지 않는다. 조기폐장일은 12:25 까지 (NYSE Rule 7.12 원문). 3단계는 언제든
        val nowEt = now.atZone(UsMarketCalendarService.KST).withZoneSameInstant(UsMarketCalendarService.NY).toLocalTime()
        if (stage < 3 && !nowEt.isBefore(usMarketCalendarService.closeTimeEt(q.tradeDate).minus(US_CB_LATE_CUTOFF))) return null
        state.cbStages.add(stage)
        return IndexAlertFact(q, AlertTrigger.CB, AlertDirection.DOWN, q.lowRate, stage = stage, firedAt = now)
    }

    /** 실시간 판정이 국내 CB 발동을 확정하면 알림 게시. 지수·단계별 하루 1회는 CircuitBreakerService 가 보장한다 */
    @EventListener
    fun onCircuitBreakerFired(e: CircuitBreakerFiredEvent) {
        val ev = e.event
        val quote = IndexQuote(ev.indexCode, ev.tradeDate, ev.triggerValue, e.prevClose, maxOf(e.high, ev.triggerValue), minOf(e.low, ev.triggerValue))
        val fact = IndexAlertFact(quote, AlertTrigger.CB, AlertDirection.DOWN, ev.triggerRate, stage = ev.stage, firedAt = ev.startedAt)
        runCatching { publish(fact, kr = true) }
            .onFailure { log.error(it) { "서킷브레이커 알림 게시 실패: ${ev.indexCode} ${ev.stage}단계" } }
    }

    private fun judgeWarn(q: IndexQuote, s: AlertStateService.IndexAlertState, now: LocalDateTime): IndexAlertFact? {
        val down = WARN_LEVELS.filter { q.lowRate <= -it && it !in s.firedDownLevels }
        val up = WARN_LEVELS.filter { q.highRate >= it && it !in s.firedUpLevels }
        val (dir, levels, basis) = when {
            down.isNotEmpty() -> Triple(AlertDirection.DOWN, down, q.lowRate)
            up.isNotEmpty() -> Triple(AlertDirection.UP, up, q.highRate)
            else -> return null
        }
        if (dir == AlertDirection.DOWN) s.firedDownLevels.addAll(levels) else s.firedUpLevels.addAll(levels)
        return IndexAlertFact(quote = q, trigger = AlertTrigger.THRESHOLD, direction = dir, triggerRate = basis, level = levels.max(), firedAt = now)
    }

    private fun publish(fact: IndexAlertFact, kr: Boolean): Int {
        val card = runCatching {
            if (kr) marketFactSheetService.collectKrIndexAlert(fact.quote.indexCode) else marketFactSheetService.collectUsIndexAlert()
        }.onFailure { log.error(it) { "알림 카드 데이터 수집 실패: ${fact.quote.indexCode}" } }.getOrNull()
            .let { withJudgedQuote(it, fact.quote) }
        val body = AlertTemplateRenderer.renderIndexAlert(fact, card)
        val entity = alertRepository.save(
            AssistantIndexAlert(
                alertDate = fact.quote.tradeDate, indexCode = fact.quote.indexCode,
                subtype = body.subtype!!, triggerType = fact.trigger.name, direction = fact.direction.name,
                stage = fact.stage, triggerRate = fact.triggerRate, currentRate = fact.quote.currentRate,
                indexValue = fact.quote.current, firedAt = fact.firedAt,
                headlineText = body.headline.text.take(300), body = objectMapper.writeValueAsString(body),
            )
        )
        val recipient: (AssistantSetting) -> Boolean = when {
            fact.trigger == AlertTrigger.CB -> { _ -> true }
            kr -> { s -> s.krWarnEnabled }
            else -> { s -> s.usWarnEnabled }
        }
        val finalBody = body.copy(refs = mapOf("alertId" to entity.id))
        val telegramText = AlertTemplateRenderer.renderIndexAlertTelegram(fact, card)
        var posted = 0
        memberRepository.findAll()
            .filter { recipient(assistantSettingService.findOrDefault(it.id)) }
            .forEach { member ->
                runCatching { timelineService.postMessage(member.id, finalBody, refAlertId = entity.id); posted++ }
                    .onSuccess {
                        runCatching { telegramNotifier.notify(member.id, telegramText) }
                            .onFailure { log.error(it) { "지수 알림 텔레그램 발송 실패: member=${member.loginId} alertId=${entity.id}" } }
                    }
                    .onFailure { log.error(it) { "지수 알림 회원 게시 실패: member=${member.loginId} alertId=${entity.id}" } }
            }
        return posted
    }

    private fun withJudgedQuote(card: IndexAlertCardFact?, q: IndexQuote): IndexAlertCardFact {
        val judged = IndexFact(AlertTemplateRenderer.indexName(q.indexCode), q.current, q.currentRate, q.current - q.prevClose, q.high, q.low)
        val base = card ?: IndexAlertCardFact(primary = null, secondary = null, flow = null)
        return if (q.indexCode == SP500) base.copy(secondary = judged.copy(delayStatus = base.secondary?.delayStatus))
        else base.copy(primary = judged.copy(delayStatus = base.primary?.delayStatus))
    }

    fun summarize(r: RunResult): String = r.quotes.joinToString(", ") { q ->
        "${q.indexCode} cur=${TemplateFormat.rate(q.currentRate, 2)} high=${TemplateFormat.rate(q.highRate, 2)} low=${TemplateFormat.rate(q.lowRate, 2)}"
    }.ifEmpty { "조회 결과 없음" } +
        if (r.fired.isEmpty()) "" else " | fired=" + r.fired.joinToString { "${it.quote.indexCode}:${it.trigger}:${it.direction}" } + " posted=${r.posted}"
}
