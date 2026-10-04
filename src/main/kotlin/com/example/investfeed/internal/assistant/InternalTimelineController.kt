package com.example.investfeed.internal.assistant

import com.example.investfeed.domain.assistant.dto.req.TurnSaveReq
import com.example.investfeed.domain.assistant.service.AssistantTurnService
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.internal.assistant.tool.ToolResponse
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/assistant/timeline")
class InternalTimelineController(
    private val assistantTurnService: AssistantTurnService,
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("turns")
    fun saveTurn(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Valid @RequestBody req: TurnSaveReq,
    ): ToolResponse<AssistantTurnService.TurnSaveRes> = try {
        ToolResponse.ok(assistantTurnService.saveTurn(user.member.id, req))
    } catch (e: Exception) {
        log.error(e) { "비서 턴 저장 실패: requestId=${req.requestId}" }
        ToolResponse.fail("턴 저장에 실패했습니다: ${e.javaClass.simpleName}")
    }
}
