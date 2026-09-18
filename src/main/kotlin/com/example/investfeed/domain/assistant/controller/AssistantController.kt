package com.example.investfeed.domain.assistant.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.assistant.dto.req.AssistantSettingReq
import com.example.investfeed.domain.assistant.dto.req.ReadMarkerReq
import com.example.investfeed.domain.assistant.dto.req.TimelineQueryReq
import com.example.investfeed.domain.assistant.dto.res.AssistantSettingRes
import com.example.investfeed.domain.assistant.dto.res.AssistantTokenRes
import com.example.investfeed.domain.assistant.service.AssistantTokenService
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

    private fun timeline(memberId: Long, req: TimelineQueryReq, includePersonal: Boolean) =
        if (req.date != null) timelineService.getTimelineByDate(memberId, req.date, includePersonal)
        else timelineService.getTimeline(memberId, req.before, req.limit, includePersonal)

    private fun <T> ok(code: ResponseCode, result: T): ResponseEntity<ApiResponse<T>> =
        ResponseEntity(ApiResponse(code = code.code, message = code.message, result = result), HttpStatus.OK)
}
