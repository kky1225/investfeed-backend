package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.message.Headline
import com.example.investfeed.domain.assistant.dto.message.HeadlineScope
import com.example.investfeed.domain.assistant.dto.message.SectionStatus
import com.example.investfeed.domain.assistant.dto.message.MessageBody
import com.example.investfeed.domain.assistant.dto.message.MessageType
import com.example.investfeed.domain.assistant.dto.req.TimelineView
import com.example.investfeed.domain.assistant.dto.res.TimelineMessageRes
import com.example.investfeed.domain.assistant.dto.res.TimelinePageRes
import com.example.investfeed.domain.assistant.entity.AssistantMessage
import com.example.investfeed.domain.assistant.repository.AssistantMessageRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import com.example.investfeed.global.constant.RedisKeyPrefix
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TimelineService(
    private val assistantMessageRepository: AssistantMessageRepository,
    private val assistantSettingService: AssistantSettingService,
    private val objectMapper: ObjectMapper,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val UNREAD_TYPES = listOf(MessageType.BRIEFING.name, MessageType.ALERT.name)
        const val MAX_LIMIT = 50
        private const val LOCKED_HEADLINE = "개인 파트 포함 (2차 인증 후 표시)"
    }

    fun getTimeline(memberId: Long, before: Long?, limit: Int, includePersonal: Boolean, view: TimelineView = TimelineView.ALL): TimelinePageRes {
        val size = limit.coerceIn(1, MAX_LIMIT)
        val page = PageRequest.of(0, size + 1)
        val types = view.types
        val rows = when {
            types == null && before == null -> assistantMessageRepository.findByMemberIdOrderByIdDesc(memberId, page)
            types == null -> assistantMessageRepository.findByMemberIdAndIdLessThanOrderByIdDesc(memberId, before!!, page)
            before == null -> assistantMessageRepository.findByMemberIdAndTypeInOrderByIdDesc(memberId, types, page)
            else -> assistantMessageRepository.findByMemberIdAndTypeInAndIdLessThanOrderByIdDesc(memberId, types, before, page)
        }
        val hasMore = rows.size > size
        val items = rows.take(size).map { toRes(it, includePersonal) }
        return TimelinePageRes(
            items = items,
            nextCursor = if (hasMore) items.last().id else null,
            unreadCount = unreadCount(memberId),
            personalIncluded = includePersonal,
        )
    }

    fun getTimelineByDate(memberId: Long, date: LocalDate, includePersonal: Boolean, view: TimelineView = TimelineView.ALL): TimelinePageRes {
        val start = date.atStartOfDay()
        val end = date.plusDays(1).atStartOfDay()
        val rows = view.types
            ?.let { assistantMessageRepository.findByMemberIdAndTypeInAndCreatedAtBetweenOrderByIdDesc(memberId, it, start, end) }
            ?: assistantMessageRepository.findByMemberIdAndCreatedAtBetweenOrderByIdDesc(memberId, start, end)
        return TimelinePageRes(
            items = rows.map { toRes(it, includePersonal) },
            nextCursor = null,
            unreadCount = unreadCount(memberId),
            personalIncluded = includePersonal,
        )
    }

    fun unreadCount(memberId: Long): Long {
        val lastSeen = assistantSettingService.findOrDefault(memberId).lastSeenMessageId
        return assistantMessageRepository.countByMemberIdAndIdGreaterThanAndTypeIn(memberId, lastSeen, UNREAD_TYPES)
    }

    fun markRead(memberId: Long, lastSeenId: Long) = assistantSettingService.markRead(memberId, lastSeenId)

    @Transactional
    fun postMessage(
        memberId: Long,
        body: MessageBody,
        refBriefingId: Long? = null,
        refPersonalBriefingId: Long? = null,
        refAlertId: Long? = null,
        requestId: String? = null,
    ): AssistantMessage {
        val entity = AssistantMessage(
            memberId = memberId,
            type = body.type.name,
            subtype = body.subtype,
            asOf = body.asOf,
            headlineText = body.headline.text.take(300),
            headlineScope = body.headline.scope.name,
            body = objectMapper.writeValueAsString(body),
            refBriefingId = refBriefingId,
            refPersonalBriefingId = refPersonalBriefingId,
            refAlertId = refAlertId,
            requestId = requestId,
        )
        return assistantMessageRepository.save(entity)
    }

    @Transactional
    fun deleteMessage(memberId: Long, messageId: Long): Boolean {
        val m = assistantMessageRepository.findByIdAndMemberId(messageId, memberId) ?: return false
        assistantMessageRepository.delete(m)
        if (m.type == MessageType.USER.name) clearChatSession(memberId)
        return true
    }

    /**
     * 보고 있는 필터의 메시지 전체 삭제 (복구 불가): ALL = 전부, BRIEFING = 브리핑·알림, CHAT = 질문·답변.
     * 질문이 지워지면 대화 기억도 초기화. 사용량 로그(본문 없음)는 남긴다
     */
    @Transactional
    fun deleteByView(memberId: Long, view: TimelineView): Int {
        val n = view.types?.let { assistantMessageRepository.deleteByMemberIdAndTypeIn(memberId, it) }
            ?: assistantMessageRepository.deleteAllByMemberId(memberId)
        if (view != TimelineView.BRIEFING) clearChatSession(memberId)
        return n
    }

    /** Python 비서의 대화 세션 키 (app/chat/session.py KEY_PREFIX) */
    private fun clearChatSession(memberId: Long) {
        runCatching { redisTemplate.delete("${RedisKeyPrefix.ASSISTANT.prefix}CHAT:$memberId") }
            .onFailure { log.error(it) { "비서 대화 세션 초기화 실패 memberId=$memberId" } }
    }

    fun isPosted(memberId: Long, briefingId: Long): Boolean =
        assistantMessageRepository.existsByMemberIdAndRefBriefingId(memberId, briefingId)

    private fun toRes(entity: AssistantMessage, includePersonal: Boolean): TimelineMessageRes {
        val body = runCatching { objectMapper.readValue<MessageBody>(entity.body) }
            .getOrElse { e ->
                log.error(e) { "assistant_message body 역직렬화 실패: id=${entity.id}" }
                MessageBody(
                    type = MessageType.SYSTEM,
                    headline = Headline(text = "메시지를 표시할 수 없습니다"),
                )
            }
        return TimelineMessageRes(
            id = entity.id,
            createdAt = entity.createdAt,
            body = if (includePersonal) body else stripPersonal(body),
        )
    }

    private fun stripPersonal(body: MessageBody): MessageBody {
        val headline = if (body.headline.scope == HeadlineScope.PERSONAL) {
            body.headline.copy(text = body.headline.publicText ?: LOCKED_HEADLINE, scope = HeadlineScope.MARKET)
        } else {
            body.headline
        }
        return body.copy(
            headline = headline,
            sections = body.sections.map { if (it.personal) it.copy(status = SectionStatus.LOCKED, text = "", summary = null, account = null, asOf = null) else it },
            turnCards = body.turnCards.map { if (it.personal) it.copy(status = SectionStatus.LOCKED, ref = null, error = null, payload = null, args = null) else it },
        )
    }
}
