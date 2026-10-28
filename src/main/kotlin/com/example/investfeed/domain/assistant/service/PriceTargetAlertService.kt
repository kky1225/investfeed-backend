package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.PriceTargetHit
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PriceTargetAlertService(
    private val timelineService: TimelineService,
    private val telegramNotifier: TelegramNotifier,
) {
    private val log = KotlinLogging.logger {}

    fun publish(hits: List<PriceTargetHit>, now: LocalDateTime): Int {
        if (hits.isEmpty()) return 0
        var posted = 0
        hits.groupBy { it.memberId }.forEach { (memberId, memberHits) ->
            runCatching {
                val body = AlertTemplateRenderer.renderPriceTargetAlert(memberHits, now)
                timelineService.postMessage(memberId, body)
                posted++
                runCatching { telegramNotifier.notify(memberId, AlertTemplateRenderer.renderPriceTargetAlertTelegram(memberHits)) }
                    .onFailure { log.error(it) { "목표가 도달 텔레그램 발송 실패: member=$memberId" } }
            }.onFailure { log.error(it) { "목표가 도달 비서 게시 실패: member=$memberId ${memberHits.map { h -> h.assetCode }}" } }
        }
        return posted
    }
}
