package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.message.Headline
import com.example.investfeed.domain.assistant.dto.message.HeadlineScope
import com.example.investfeed.domain.assistant.dto.message.SectionStatus
import com.example.investfeed.domain.assistant.dto.message.MessageBody
import com.example.investfeed.domain.assistant.dto.message.MessageType
import com.example.investfeed.domain.assistant.dto.res.TimelineMessageRes
import com.example.investfeed.domain.assistant.dto.res.TimelinePageRes
import com.example.investfeed.domain.assistant.entity.AssistantMessage
import com.example.investfeed.domain.assistant.repository.AssistantMessageRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TimelineService(
    private val assistantMessageRepository: AssistantMessageRepository,
    private val assistantSettingService: AssistantSettingService,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val UNREAD_TYPES = listOf(MessageType.BRIEFING.name, MessageType.ALERT.name)
        const val MAX_LIMIT = 50
        private const val LOCKED_HEADLINE = "개인 파트 포함 (2차 인증 후 표시)"
    }

    fun getTimeline(memberId: Long, before: Long?, limit: Int, includePersonal: Boolean): TimelinePageRes {
        val size = limit.coerceIn(1, MAX_LIMIT)
        val page = PageRequest.of(0, size + 1)
        val rows = if (before == null) {
            assistantMessageRepository.findByMemberIdOrderByIdDesc(memberId, page)
        } else {
            assistantMessageRepository.findByMemberIdAndIdLessThanOrderByIdDesc(memberId, before, page)
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

    fun getTimelineByDate(memberId: Long, date: LocalDate, includePersonal: Boolean): TimelinePageRes {
        val rows = assistantMessageRepository.findByMemberIdAndCreatedAtBetweenOrderByIdDesc(
            memberId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()
        )
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
        )
    }
}
