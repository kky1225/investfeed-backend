package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.assistant.dto.message.ActionStatus
import com.example.investfeed.domain.assistant.dto.message.Headline
import com.example.investfeed.domain.assistant.dto.message.HeadlineScope
import com.example.investfeed.domain.assistant.dto.message.MessageBody
import com.example.investfeed.domain.assistant.dto.message.MessageType
import com.example.investfeed.domain.assistant.service.TimelineService
import com.example.investfeed.domain.auth.entity.Member
import com.example.investfeed.domain.goal.dto.req.InvestmentGoalCreateReq
import com.example.investfeed.domain.goal.dto.req.InvestmentGoalUpdateReq
import com.example.investfeed.domain.goal.dto.res.InvestmentGoalRes
import com.example.investfeed.domain.goal.entity.GoalType
import com.example.investfeed.domain.goal.repository.InvestmentGoalRepository
import com.example.investfeed.domain.goal.service.InvestmentGoalService
import com.example.investfeed.domain.notification.dto.req.PriceTargetCreateReq
import com.example.investfeed.domain.notification.dto.res.PriceTargetRes
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.PriceTargetDirection
import com.example.investfeed.domain.notification.service.PriceTargetService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.util.Locale

@Service
class ActionToolService(
    private val stockResolver: StockResolver,
    private val priceTargetService: PriceTargetService,
    private val cardStore: CardStore,
    private val objectMapper: ObjectMapper,
    private val investmentGoalRepository: InvestmentGoalRepository,
    private val investmentGoalService: InvestmentGoalService,
    private val timelineService: TimelineService,
) {
    companion object {
        const val KIND_CONFIRM_PRICE_ALERT = "CONFIRM_PRICE_ALERT"
        const val KIND_CONFIRM_PRICE_ALERT_DELETE = "CONFIRM_PRICE_ALERT_DELETE"
        const val KIND_CONFIRM_GOAL = "CONFIRM_GOAL"
        const val ACTION_RESULT = "ACTION_RESULT"   // 확인 카드 처리 결과 답변의 subtype
    }

    data class PriceAlertPreview(
        val asOf: LocalDateTime, val code: String, val name: String, val market: StockMarket, val assetCode: String,
        val price: Double, val direction: PriceTargetDirection, val currency: String,   // 국내·코인 원, 미국 달러(USD)
        val confirmRef: String? = null,  // 등록 버튼이 보내는 확인 카드 참조 (Redis 원본으로 등록해 화면 값 변조를 막는다)
    )

    fun previewPriceAlert(member: Member, stockQuery: String, price: Double, direction: PriceTargetDirection): PriceAlertPreview {
        if (price <= 0) throw ToolException("목표가는 0보다 커야 합니다")
        val s = stockResolver.resolve(stockQuery, member.id)
        val currency = if (s.market == StockMarket.US) "USD" else "KRW"
        val preview = PriceAlertPreview(LocalDateTime.now(), s.code, s.name, s.market, s.assetCode, price, direction, currency)
        return preview.copy(confirmRef = cardStore.put(member.id, KIND_CONFIRM_PRICE_ALERT, preview).cardRef)
    }

    fun confirmPriceAlert(member: Member, cardRef: String): PriceTargetRes {
        val card = cardStore.get(member.id, cardRef)?.takeIf { it.kind == KIND_CONFIRM_PRICE_ALERT }
            ?: throw ToolException("확인 카드가 만료되었거나 이미 등록되었습니다")
        val p = objectMapper.convertValue(card.payload, PriceAlertPreview::class.java)
        val res = priceTargetService.createPriceTarget(
            member.id,
            PriceTargetCreateReq(
                assetType = if (p.market == StockMarket.CRYPTO) AssetType.CRYPTO else AssetType.STOCK,
                assetCode = p.assetCode, assetName = p.name, targetPrice = BigDecimal.valueOf(p.price), direction = p.direction,
            ),
        )
        cardStore.delete(cardRef)
        finish(member, cardRef, ActionStatus.CONFIRMED, Headline("${p.name} ${condition(p.price, p.currency, p.direction)} 도달 시 알림을 등록했습니다."))
        return res
    }

    fun cancelPriceAlert(member: Member, cardRef: String) {
        cardStore.get(member.id, cardRef)?.takeIf { it.kind == KIND_CONFIRM_PRICE_ALERT }
            ?: throw ToolException("확인 카드가 만료되었거나 이미 처리되었습니다")
        cardStore.delete(cardRef)
        finish(member, cardRef, ActionStatus.CANCELED, Headline("목표가 알림 등록을 취소했습니다."))
    }

    /** 삭제 확인 카드의 알림 한 줄 (등록된 목표가 알림) */
    data class PriceAlertRow(
        val id: Long, val code: String, val name: String, val assetCode: String,
        val price: Double, val direction: PriceTargetDirection, val currency: String,
    )

    data class PriceAlertDeletePreview(
        val asOf: LocalDateTime, val alerts: List<PriceAlertRow>,
        val notice: String? = null,      // 말한 목표가의 알림이 없어 그 종목 알림을 모두 보여 줄 때
        val confirmRef: String? = null,  // 삭제 버튼이 보내는 확인 카드 참조 (Redis 원본에 있는 알림만 삭제)
    )

    /**
     * 목표가 알림 삭제 확인 카드 (2026-10-28). 종목은 전체 종목 마스터가 아니라 내가 등록한 알림 안에서 찾는다
     * — 이름이 정확하지 않아도("2차전지레버리지") 등록된 "KODEX 2차전지산업레버리지"를 바로 찾는다. 확인 카드가 곧 되묻기라 후보 질문은 없다.
     * 여러 개면 카드에서 하나를 골라 삭제한다. 알림이 없으면 카드 없이 문장으로 답한다
     */
    fun previewPriceAlertDelete(member: Member, stockQuery: String, price: Double?): Any {
        val q = stockQuery.trim()
        if (q.isEmpty()) throw ToolException("종목을 입력해 주세요")
        val all = priceTargetService.getPriceTargets(member.id)
        if (all.isEmpty()) return DataToolService.TextReply("등록된 목표가 알림이 없습니다.")

        val byStock = matchAlerts(member, q, all)
        if (byStock.isEmpty()) return DataToolService.TextReply("'$q'에 해당하는 목표가 알림이 없습니다.")
        val byPrice = price?.let { p -> byStock.filter { it.targetPrice.compareTo(BigDecimal.valueOf(p)) == 0 } }.orEmpty()
        val notice = if (price != null && byPrice.isEmpty()) {
            "${priceText(price, currencyOf(byStock.first().assetCode))} 목표가 알림은 없어 등록된 알림을 모두 보여 드립니다."
        } else null

        val rows = byPrice.ifEmpty { byStock }.map {
            PriceAlertRow(
                it.id, it.assetCode.substringBefore("_"), it.assetName, it.assetCode,
                it.targetPrice.toDouble(), it.direction, currencyOf(it.assetCode),
            )
        }
        val preview = PriceAlertDeletePreview(LocalDateTime.now(), rows, notice)
        return preview.copy(confirmRef = cardStore.put(member.id, KIND_CONFIRM_PRICE_ALERT_DELETE, preview).cardRef)
    }

    /** 내 알림 중 종목 찾기: 종목 해석(코드·이름·별칭) → 이름 포함 → 단어별 포함 → 비슷한 이름 순 */
    private fun matchAlerts(member: Member, q: String, all: List<PriceTargetRes>): List<PriceTargetRes> {
        runCatching { stockResolver.resolve(q, member.id) }.getOrNull()
            ?.let { s -> all.filter { it.assetCode == s.assetCode } }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        val nq = stockResolver.norm(q)
        all.filter { stockResolver.norm(it.assetName).contains(nq) || it.assetCode.substringBefore("_").equals(q, ignoreCase = true) }
            .takeIf { it.isNotEmpty() }
            ?.let { return it }

        val tokens = q.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        all.filter { a -> tokens.all { stockResolver.norm(a.assetName).contains(it) } }
            .takeIf { it.isNotEmpty() }
            ?.let { return it }

        val scored = all.map { it to stockResolver.similarity(nq, stockResolver.norm(it.assetName)) }
            .filter { (_, score) -> score >= StockResolver.MIN_SIMILARITY }
        val best = scored.maxOfOrNull { (_, score) -> score } ?: return emptyList()
        val bestCode = scored.first { (_, score) -> score == best }.first.assetCode
        return scored.map { it.first }.filter { it.assetCode == bestCode }   // 가장 비슷한 종목 하나의 알림만
    }

    fun confirmPriceAlertDelete(member: Member, cardRef: String, targetId: Long) {
        val card = cardStore.get(member.id, cardRef)?.takeIf { it.kind == KIND_CONFIRM_PRICE_ALERT_DELETE }
            ?: throw ToolException("확인 카드가 만료되었거나 이미 삭제되었습니다")
        val p = objectMapper.convertValue(card.payload, PriceAlertDeletePreview::class.java)
        val row = p.alerts.firstOrNull { it.id == targetId } ?: throw ToolException("이 카드에 없는 알림입니다")
        try {
            priceTargetService.deletePriceTarget(member.id, row.id)
        } catch (e: IllegalArgumentException) {
            throw ToolException("이미 삭제되었거나 목표가에 도달해 알림이 발송된 알림입니다")
        }
        cardStore.delete(cardRef)
        finish(member, cardRef, ActionStatus.CONFIRMED, Headline("${row.name} ${condition(row.price, row.currency, row.direction)} 목표가 알림을 삭제했습니다."))
    }

    fun cancelPriceAlertDelete(member: Member, cardRef: String) {
        cardStore.get(member.id, cardRef)?.takeIf { it.kind == KIND_CONFIRM_PRICE_ALERT_DELETE }
            ?: throw ToolException("확인 카드가 만료되었거나 이미 처리되었습니다")
        cardStore.delete(cardRef)
        finish(member, cardRef, ActionStatus.CANCELED, Headline("목표가 알림 삭제를 취소했습니다."))
    }

    private fun currencyOf(assetCode: String) = if (assetCode.endsWith("_US")) "USD" else "KRW"

    private fun condition(price: Double, currency: String, direction: PriceTargetDirection) =
        "${priceText(price, currency)} ${if (direction == PriceTargetDirection.ABOVE) "이상" else "이하"}"

    private fun priceText(v: Double, currency: String): String =
        if (currency == "USD") "$" + DecimalFormat("#,##0.00##", DecimalFormatSymbols(Locale.US)).format(v)
        else DecimalFormat("#,##0.###", DecimalFormatSymbols(Locale.US)).format(v) + "원"

    data class GoalUpdatePreview(val asOf: LocalDateTime, val type: GoalType, val currentTarget: Long?, val newTarget: Long)

    fun previewGoalUpdate(member: Member, type: GoalType, targetAmount: Long): CardRef {
        if (targetAmount <= 0) throw ToolException("목표 금액은 0보다 커야 합니다")
        val current = investmentGoalRepository.findByMemberIdAndType(member.id, type)?.targetAmount
        if (current == targetAmount) throw ToolException("이미 같은 목표 금액입니다")
        return cardStore.put(member.id, KIND_CONFIRM_GOAL, GoalUpdatePreview(LocalDateTime.now(), type, current, targetAmount))
    }

    fun confirmGoalUpdate(member: Member, cardRef: String): InvestmentGoalRes {
        val card = cardStore.get(member.id, cardRef)?.takeIf { it.kind == KIND_CONFIRM_GOAL }
            ?: throw ToolException("확인 카드가 만료되었거나 이미 변경되었습니다")
        val p = objectMapper.convertValue(card.payload, GoalUpdatePreview::class.java)
        val res = investmentGoalRepository.findByMemberIdAndType(member.id, p.type)
            ?.let { investmentGoalService.updateGoal(it.id, InvestmentGoalUpdateReq(p.newTarget)) }
            ?: investmentGoalService.createGoal(InvestmentGoalCreateReq(p.type, p.newTarget))
        cardStore.delete(cardRef)
        // 금액이 들어가 개인 문장 — 2차 인증 전에는 금액 없는 문장으로 보인다
        val done = if (p.currentTarget == null) "설정했습니다" else "변경했습니다"
        finish(member, cardRef, ActionStatus.CONFIRMED, Headline(
            "${goalLabel(p.type)}를 ${"%,d".format(p.newTarget)}원으로 $done.", HeadlineScope.PERSONAL, "투자 목표를 $done.",
        ))
        return res
    }

    fun cancelGoalUpdate(member: Member, cardRef: String) {
        cardStore.get(member.id, cardRef)?.takeIf { it.kind == KIND_CONFIRM_GOAL }
            ?: throw ToolException("확인 카드가 만료되었거나 이미 처리되었습니다")
        cardStore.delete(cardRef)
        finish(member, cardRef, ActionStatus.CANCELED, Headline("투자 목표 변경을 취소했습니다."))
    }

    private fun goalLabel(type: GoalType) = when (type) {
        GoalType.TOTAL_ASSET -> "총 자산 목표"
        GoalType.MONTHLY_REALIZED_PNL -> "월간 실현손익 목표"
        GoalType.YEARLY_REALIZED_PNL -> "연간 실현손익 목표"
    }

    private fun finish(member: Member, cardRef: String, status: ActionStatus, headline: Headline) {
        timelineService.markActionCard(member.id, cardRef, status)
        timelineService.postMessage(
            member.id,
            MessageBody(type = MessageType.ASSISTANT, subtype = ACTION_RESULT, asOf = LocalDateTime.now(), headline = headline),
        )
    }
}
