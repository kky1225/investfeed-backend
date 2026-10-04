package com.example.investfeed.domain.assistant.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.assistant.dto.req.AssistantSettingReq
import com.example.investfeed.domain.assistant.dto.req.ReadMarkerReq
import com.example.investfeed.domain.assistant.dto.req.TelegramStatusReq
import com.example.investfeed.domain.assistant.dto.req.TimelineQueryReq
import com.example.investfeed.domain.assistant.dto.req.TimelineView
import com.example.investfeed.domain.assistant.dto.res.AssistantSettingRes
import com.example.investfeed.domain.assistant.dto.res.AssistantTokenRes
import com.example.investfeed.domain.assistant.dto.res.TelegramLinkCodeRes
import com.example.investfeed.domain.assistant.dto.res.TelegramStatusRes
import com.example.investfeed.domain.assistant.service.AssistantTokenService
import com.example.investfeed.domain.assistant.service.TelegramLinkService
import com.example.investfeed.domain.assistant.dto.req.PriceAlertActionReq
import com.example.investfeed.domain.notification.dto.res.PriceTargetRes
import com.example.investfeed.internal.assistant.tool.ActionToolService
import com.example.investfeed.internal.assistant.tool.CardStore
import com.example.investfeed.internal.assistant.tool.ToolException
import java.time.LocalDateTime
import java.time.ZoneId
import com.example.investfeed.domain.assistant.dto.res.TimelinePageRes
import com.example.investfeed.domain.assistant.service.AssistantSettingService
import com.example.investfeed.domain.assistant.service.TimelineService
import com.example.investfeed.domain.security.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RequiresAction(permission = Permissions.ASSISTANT)
@RestController
@RequestMapping("/api/assistant")
class AssistantController(
    private val timelineService: TimelineService,
    private val assistantSettingService: AssistantSettingService,
    private val assistantTokenService: AssistantTokenService,
    private val telegramLinkService: TelegramLinkService,
    private val cardStore: CardStore,
    private val actionToolService: ActionToolService,
) {

    @GetMapping("timeline")
    @RequiresAction(action = Actions.READ)
    fun getTimeline(
        @AuthenticationPrincipal user: CustomUserDetails,
        @ModelAttribute req: TimelineQueryReq,
    ): ResponseEntity<ApiResponse<TimelinePageRes>> =
        ok(ResponseCode.ASSISTANT_TIMELINE_LIST, timeline(user.member.id, req, includePersonal = false))

    @GetMapping("secure/timeline")
    @RequiresAction(action = Actions.READ)
    fun getSecureTimeline(
        @AuthenticationPrincipal user: CustomUserDetails,
        @ModelAttribute req: TimelineQueryReq,
    ): ResponseEntity<ApiResponse<TimelinePageRes>> =
        ok(ResponseCode.ASSISTANT_TIMELINE_LIST, timeline(user.member.id, req, includePersonal = true))

    @GetMapping("timeline/unread-count")
    @RequiresAction(action = Actions.READ)
    fun getUnreadCount(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ResponseEntity<ApiResponse<Long>> =
        ok(ResponseCode.ASSISTANT_UNREAD_COUNT, timelineService.unreadCount(user.member.id))

    @PatchMapping("timeline/read-marker")
    @RequiresAction(action = Actions.UPDATE)
    fun updateReadMarker(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Valid @RequestBody req: ReadMarkerReq,
    ): ResponseEntity<ApiResponse<Long>> {
        timelineService.markRead(user.member.id, req.lastSeenId)
        return ok(ResponseCode.ASSISTANT_READ_MARKER_UPDATE, timelineService.unreadCount(user.member.id))
    }

    @DeleteMapping("timeline/messages/{id}")
    @RequiresAction(action = Actions.DELETE)
    fun deleteMessage(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Long?>> {
        if (!timelineService.deleteMessage(user.member.id, id)) {
            return ResponseEntity(ApiResponse(code = ResponseCode.ASSISTANT_MESSAGE_DELETE.code, message = "메시지가 없거나 이미 삭제되었습니다.", result = null), HttpStatus.NOT_FOUND)
        }
        return ok(ResponseCode.ASSISTANT_MESSAGE_DELETE, id)
    }

    @DeleteMapping("timeline/messages")
    @RequiresAction(action = Actions.DELETE)
    fun deleteMessages(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam view: TimelineView,
    ): ResponseEntity<ApiResponse<Int>> =
        ok(ResponseCode.ASSISTANT_MESSAGES_DELETE, timelineService.deleteByView(user.member.id, view))

    @PostMapping("secure/token")
    @RequiresAction(action = Actions.CREATE)
    fun issueToken(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ResponseEntity<ApiResponse<AssistantTokenRes>> {
        val issued = assistantTokenService.issue(user.member.id)
        return ok(
            ResponseCode.ASSISTANT_TOKEN_ISSUE,
            AssistantTokenRes(token = issued.token, expiresAt = LocalDateTime.ofInstant(issued.expiresAt, ZoneId.systemDefault()), kid = issued.kid),
        )
    }

    @GetMapping("settings")
    @RequiresAction(action = Actions.READ)
    fun getSettings(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ResponseEntity<ApiResponse<AssistantSettingRes>> =
        ok(ResponseCode.ASSISTANT_SETTING_GET, assistantSettingService.getSetting(user.member.id))

    @PutMapping("settings")
    @RequiresAction(action = Actions.UPDATE)
    fun updateSettings(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Valid @RequestBody req: AssistantSettingReq,
    ): ResponseEntity<ApiResponse<AssistantSettingRes>> =
        ok(ResponseCode.ASSISTANT_SETTING_UPDATE, assistantSettingService.saveSetting(user.member.id, req))

    @GetMapping("telegram")
    @RequiresAction(action = Actions.READ)
    fun getTelegram(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ResponseEntity<ApiResponse<TelegramStatusRes>> =
        ok(ResponseCode.ASSISTANT_TELEGRAM_STATUS, telegramLinkService.status(user.member.id))

    @PostMapping("telegram/link-code")
    @RequiresAction(action = Actions.CREATE)
    fun issueTelegramLinkCode(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ResponseEntity<ApiResponse<TelegramLinkCodeRes>> =
        ok(ResponseCode.ASSISTANT_TELEGRAM_LINK_CODE, telegramLinkService.issueLinkCode(user.member.id))

    @PatchMapping("telegram")
    @RequiresAction(action = Actions.UPDATE)
    fun updateTelegram(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Valid @RequestBody req: TelegramStatusReq,
    ): ResponseEntity<ApiResponse<TelegramStatusRes>> =
        ok(ResponseCode.ASSISTANT_TELEGRAM_UPDATE, telegramLinkService.setStatus(user.member.id, req.status))

    @DeleteMapping("telegram")
    @RequiresAction(action = Actions.DELETE)
    fun unlinkTelegram(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ResponseEntity<ApiResponse<TelegramStatusRes>> =
        ok(ResponseCode.ASSISTANT_TELEGRAM_UNLINK, telegramLinkService.unlink(user.member.id))

    @GetMapping("secure/cards/{ref}")
    @RequiresAction(action = Actions.READ)
    fun getCard(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable ref: String,
    ): ResponseEntity<ApiResponse<CardStore.StoredCard?>> {
        val card = cardStore.get(user.member.id, ref)
            ?: return ResponseEntity(ApiResponse(code = ResponseCode.ASSISTANT_CARD_GET.code, message = "카드가 없거나 만료되었습니다.", result = null), HttpStatus.NOT_FOUND)
        return ok(ResponseCode.ASSISTANT_CARD_GET, card)
    }

    @PostMapping("secure/actions/price-alert")
    @RequiresAction(action = Actions.CREATE)
    fun confirmPriceAlert(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Valid @RequestBody req: PriceAlertActionReq,
    ): ResponseEntity<ApiResponse<PriceTargetRes?>> = try {
        ok(ResponseCode.ASSISTANT_PRICE_ALERT_CREATE, actionToolService.confirmPriceAlert(user.member, req.cardRef))
    } catch (e: ToolException) {
        ResponseEntity(ApiResponse(code = ResponseCode.ASSISTANT_PRICE_ALERT_CREATE.code, message = e.message ?: "등록할 수 없습니다.", result = null), HttpStatus.NOT_FOUND)
    }

    private fun timeline(memberId: Long, req: TimelineQueryReq, includePersonal: Boolean) =
        if (req.date != null) timelineService.getTimelineByDate(memberId, req.date, includePersonal, req.view)
        else timelineService.getTimeline(memberId, req.before, req.limit, includePersonal, req.view)

    private fun <T> ok(code: ResponseCode, result: T): ResponseEntity<ApiResponse<T>> =
        ResponseEntity(ApiResponse(code = code.code, message = code.message, result = result), HttpStatus.OK)
}
