package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.auth.entity.Member
import com.example.investfeed.domain.notification.dto.req.PriceTargetCreateReq
import com.example.investfeed.domain.notification.dto.res.PriceTargetRes
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.PriceTargetDirection
import com.example.investfeed.domain.notification.service.PriceTargetService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ActionToolService(
    private val stockResolver: StockResolver,
    private val priceTargetService: PriceTargetService,
    private val cardStore: CardStore,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        const val KIND_CONFIRM_PRICE_ALERT = "CONFIRM_PRICE_ALERT"
    }

    data class PriceAlertPreview(
        val asOf: LocalDateTime, val code: String, val name: String, val market: StockMarket, val assetCode: String,
        val price: Long, val direction: PriceTargetDirection, val currency: String,
    )

    fun previewPriceAlert(member: Member, stockQuery: String, price: Long, direction: PriceTargetDirection): CardRef {
        if (price <= 0) throw ToolException("목표가는 0보다 커야 합니다")
        val s = stockResolver.resolve(stockQuery, member.id)
        if (s.market == StockMarket.US) throw ToolException("목표가 알림은 국내 주식·코인만 등록할 수 있습니다")
        val preview = PriceAlertPreview(LocalDateTime.now(), s.code, s.name, s.market, s.assetCode, price, direction, "KRW")
        return cardStore.put(member.id, KIND_CONFIRM_PRICE_ALERT, preview)
    }

    fun confirmPriceAlert(member: Member, cardRef: String): PriceTargetRes {
        val card = cardStore.get(member.id, cardRef)?.takeIf { it.kind == KIND_CONFIRM_PRICE_ALERT }
            ?: throw ToolException("확인 카드가 만료되었거나 이미 등록되었습니다")
        val p = objectMapper.convertValue(card.payload, PriceAlertPreview::class.java)
        val res = priceTargetService.createPriceTarget(
            member.id,
            PriceTargetCreateReq(
                assetType = if (p.market == StockMarket.CRYPTO) AssetType.CRYPTO else AssetType.STOCK,
                assetCode = p.assetCode, assetName = p.name, targetPrice = p.price, direction = p.direction,
            ),
        )
        cardStore.delete(cardRef)
        return res
    }
}
