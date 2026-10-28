package com.example.investfeed.domain.assistant.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.assistant.dto.req.GoalActionReq
import com.example.investfeed.domain.assistant.dto.req.TimelineQueryReq
import com.example.investfeed.domain.assistant.dto.res.AssistantTokenRes
import com.example.investfeed.domain.assistant.dto.res.TimelinePageRes
import com.example.investfeed.domain.assistant.service.AssistantTokenService
import com.example.investfeed.domain.assistant.service.TimelineService
import com.example.investfeed.domain.goal.dto.res.InvestmentGoalRes
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.internal.assistant.tool.ActionToolService
import com.example.investfeed.internal.assistant.tool.CardStore
import com.example.investfeed.internal.assistant.tool.ToolException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * AI 비서 개인 파트 (2차 인증 필요). SecondaryAuthFilter 가 "/api/assistant/secure/" 경로 전체를 보호한다.
 * 개인정보를 다루는 비서 API 는 이 컨트롤러에 추가할 것 — 시장 파트(로그인만)는 AssistantController
 */
@RequiresAction(permission = Permissions.ASSISTANT)
@RestController
@RequestMapping("/api/assistant/secure")
class AssistantSecureController(
    private val timelineService: TimelineService,
    private val assistantTokenService: AssistantTokenService,
    private val cardStore: CardStore,
    private val actionToolService: ActionToolService,
) {

    @GetMapping("timeline")
    @RequiresAction(action = Actions.READ)
    fun getSecureTimeline(
        @AuthenticationPrincipal user: CustomUserDetails,
        @ModelAttribute req: TimelineQueryReq,
    ): ResponseEntity<ApiResponse<TimelinePageRes>> {
        val memberId = user.member.id
        val page = if (req.date != null) timelineService.getTimelineByDate(memberId, req.date, true, req.view)
        else timelineService.getTimeline(memberId, req.before, req.limit, true, req.view)
        return ok(ResponseCode.ASSISTANT_TIMELINE_LIST, page)
    }

    @PostMapping("token")
    @RequiresAction(action = Actions.CREATE)
    fun issueToken(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ResponseEntity<ApiResponse<AssistantTokenRes>> {
        val issued = assistantTokenService.issue(user.member.id, AssistantTokenService.SCOPE_CHAT)
        return ok(
            ResponseCode.ASSISTANT_TOKEN_ISSUE,
            AssistantTokenRes(token = issued.token, expiresAt = LocalDateTime.ofInstant(issued.expiresAt, ZoneId.systemDefault()), kid = issued.kid),
        )
    }

    @GetMapping("cards/{ref}")
    @RequiresAction(action = Actions.READ)
    fun getCard(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable ref: String,
    ): ResponseEntity<ApiResponse<CardStore.StoredCard?>> {
        val card = cardStore.get(user.member.id, ref)
            ?: return ResponseEntity(ApiResponse(code = ResponseCode.ASSISTANT_CARD_GET.code, message = "카드가 없거나 만료되었습니다.", result = null), HttpStatus.NOT_FOUND)
        return ok(ResponseCode.ASSISTANT_CARD_GET, card)
    }

    @PostMapping("actions/goal")
    @RequiresAction(action = Actions.UPDATE)
    fun confirmGoalUpdate(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Valid @RequestBody req: GoalActionReq,
    ): ResponseEntity<ApiResponse<InvestmentGoalRes?>> = try {
        ok(ResponseCode.ASSISTANT_GOAL_UPDATE, actionToolService.confirmGoalUpdate(user.member, req.cardRef))
    } catch (e: ToolException) {
        ResponseEntity(ApiResponse(code = ResponseCode.ASSISTANT_GOAL_UPDATE.code, message = e.message ?: "변경할 수 없습니다.", result = null), HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("actions/goal/{cardRef}")
    @RequiresAction(action = Actions.DELETE)
    fun cancelGoalUpdate(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable cardRef: String,
    ): ResponseEntity<ApiResponse<Nothing?>> = try {
        actionToolService.cancelGoalUpdate(user.member, cardRef)
        ok(ResponseCode.ASSISTANT_ACTION_CANCEL, null)
    } catch (e: ToolException) {
        ResponseEntity(ApiResponse(code = ResponseCode.ASSISTANT_ACTION_CANCEL.code, message = e.message ?: "취소할 수 없습니다.", result = null), HttpStatus.NOT_FOUND)
    }

    private fun <T> ok(code: ResponseCode, result: T): ResponseEntity<ApiResponse<T>> =
        ResponseEntity(ApiResponse(code = code.code, message = code.message, result = result), HttpStatus.OK)
}
