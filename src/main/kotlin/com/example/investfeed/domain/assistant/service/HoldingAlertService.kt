package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.HoldingAlertHit
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class HoldingAlertService(
    private val timelineService: TimelineService,
    private val telegramNotifier: TelegramNotifier,
) {
    private val log = KotlinLogging.logger {}

    fun publish(hits: List<HoldingAlertHit>, now: LocalDateTime): Int {
        if (hits.isEmpty()) return 0
        var posted = 0
        hits.groupBy { it.memberId }.forEach { (memberId, memberHits) ->
            runCatching {
                val body = AlertTemplateRenderer.renderHoldingAlert(memberHits, now)
                timelineService.postMessage(memberId, body)
                posted++
                runCatching { telegramNotifier.notify(memberId, AlertTemplateRenderer.renderHoldingAlertTelegram(memberHits)) }
                    .onFailure { log.error(it) { "보유 종목 알림 텔레그램 발송 실패: member=$memberId" } }
            }.onFailure { log.error(it) { "보유 종목 알림 게시 실패: member=$memberId ${memberHits.map { h -> h.assetCode }}" } }
        }
        return posted
    }
}
