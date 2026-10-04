package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.message.Headline
import com.example.investfeed.domain.assistant.dto.message.MessageBody
import com.example.investfeed.domain.assistant.dto.message.MessageType
import com.example.investfeed.domain.assistant.dto.message.SectionStatus
import com.example.investfeed.domain.assistant.dto.message.TurnCard
import com.example.investfeed.domain.assistant.dto.req.TurnCardReq
import com.example.investfeed.domain.assistant.dto.req.TurnSaveReq
import com.example.investfeed.domain.assistant.entity.AssistantChatLog
import com.example.investfeed.domain.assistant.repository.AssistantChatLogRepository
import com.example.investfeed.internal.assistant.tool.ActionToolService
import com.example.investfeed.internal.assistant.tool.CardStore
import com.example.investfeed.internal.assistant.tool.DisplayToolService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AssistantTurnService(
    private val timelineService: TimelineService,
    private val cardStore: CardStore,
    private val chatLogRepository: AssistantChatLogRepository,
) {
    companion object {
        val PERSONAL_KINDS = setOf(
            DisplayToolService.KIND_PORTFOLIO,
            DisplayToolService.KIND_HOLDING,
            DisplayToolService.KIND_PNL,
            ActionToolService.KIND_CONFIRM_PRICE_ALERT,
            "show_briefing",
        )
        private const val EXPIRED_CARD = "카드가 만료되어 저장하지 못했습니다"
    }

    data class TurnSaveRes(val userMessageId: Long?, val assistantMessageId: Long?, val duplicated: Boolean = false)

    @Transactional
    fun saveTurn(memberId: Long, req: TurnSaveReq): TurnSaveRes {
        if (chatLogRepository.existsByRequestId(req.requestId)) return TurnSaveRes(null, null, duplicated = true)

        val now = LocalDateTime.now()
        val user = timelineService.postMessage(
            memberId,
            MessageBody(type = MessageType.USER, asOf = now, headline = Headline(text = req.question)),
            requestId = req.requestId,
        )
        val cards = req.cards.map { toTurnCard(memberId, it) }
        val assistant = timelineService.postMessage(
            memberId,
            MessageBody(
                type = MessageType.ASSISTANT,
                subtype = req.log.route,
                asOf = now,
                headline = Headline(text = req.replyText),
                turnCards = cards,
            ),
            requestId = req.requestId,
        )
        chatLogRepository.save(req.log.let {
            AssistantChatLog(
                memberId = memberId,
                requestId = req.requestId,
                route = it.route,
                reason = it.reason,
                tools = it.tools.joinToString(",").takeIf { s -> s.isNotEmpty() }?.take(300),
                toolErrorCount = it.toolErrorCount.toShort(),
                llmCalls = it.llmCalls.toShort(),
                model = it.model,
                inputTokens = it.inputTokens,
                outputTokens = it.outputTokens,
                cacheReadTokens = it.cacheReadTokens,
                cacheWriteTokens = it.cacheWriteTokens,
                costUsd = it.costUsd,
                compacted = it.compacted,
                latencyMs = it.latencyMs,
            )
        })
        return TurnSaveRes(user.id, assistant.id)
    }

    private fun toTurnCard(memberId: Long, c: TurnCardReq): TurnCard {
        if (c.ref == null) {
            return TurnCard(
                kind = c.kind,
                personal = c.kind in PERSONAL_KINDS,
                status = if (c.error != null) SectionStatus.FAILED else SectionStatus.OK,
                asOf = c.asOf, source = c.source, error = c.error, payload = c.payload, args = c.args,
            )
        }
        val stored = cardStore.get(memberId, c.ref)
            ?: return TurnCard(kind = c.kind, personal = true, status = SectionStatus.FAILED, error = EXPIRED_CARD)
        return TurnCard(
            kind = stored.kind,
            personal = stored.kind in PERSONAL_KINDS,
            ref = stored.ref,
            asOf = stored.createdAt,
            payload = stored.payload,
        )
    }
}
