package com.example.investfeed.domain.assistant.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.assistant.entity.AssistantChatLog
import com.example.investfeed.domain.assistant.enum.ChatLogLabel
import com.example.investfeed.domain.assistant.repository.AssistantChatLogRepository
import com.example.investfeed.domain.auth.repository.MemberRepository
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@RequiresAction(permission = Permissions.ADMIN_MONITORING)
@RestController
@RequestMapping("/api/admin/assistant/usage")
class AssistantUsageAdminController(
    private val chatLogRepository: AssistantChatLogRepository,
    private val memberRepository: MemberRepository,
) {
    data class UsageSum(
        val turns: Int,
        val llmCalls: Int,
        val inputTokens: Long,
        val outputTokens: Long,
        val cacheReadTokens: Long,
        val costUsd: BigDecimal,
        val rejectCount: Int,
        val askCount: Int,
        val errorCount: Int,   // ERROR + BLOCKED
        val avgLatencyMs: Int, // 턴 평균 응답 시간
    )

    data class DailyUsage(val date: LocalDate, val usage: UsageSum)
    data class MemberUsage(val memberId: Long, val loginId: String?, val usage: UsageSum)   // 식별은 아이디만 (실명·닉네임 미노출, 2026-10-04)
    data class UsageRes(val month: String, val total: UsageSum, val daily: List<DailyUsage>, val members: List<MemberUsage>)

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun usage(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth?): ResponseEntity<ApiResponse<UsageRes>> {
        val ym = month ?: YearMonth.now()
        val logs = chatLogRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay(),
        )
        val memberById = memberRepository.findAllById(logs.map { it.memberId }.distinct()).associateBy { it.id }
        val res = UsageRes(
            month = ym.toString(),
            total = sum(logs),
            daily = logs.groupBy { it.createdAt.toLocalDate() }.toSortedMap().map { (d, l) -> DailyUsage(d, sum(l)) },
            members = logs.groupBy { it.memberId }.map { (id, l) -> memberById[id].let { m -> MemberUsage(id, m?.loginId, sum(l)) } }.sortedByDescending { it.usage.costUsd },
        )
        return ResponseEntity(ApiResponse(code = ResponseCode.ASSISTANT_USAGE_GET.code, message = ResponseCode.ASSISTANT_USAGE_GET.message, result = res), HttpStatus.OK)
    }

    /** 처리 기록 1건 — 질문 원문은 포함하지 않는다 (개인정보 최소 열람, 2026-10-04) */
    data class ChatLogRes(
        val id: Long,
        val createdAt: LocalDateTime,
        val route: String,
        val reason: String?,
        val tools: List<String>,
        val routeLabel: String,           // 화면 문구 (ChatLogLabel) — 원래 코드는 위 필드에 그대로 둔다
        val requestLabel: String?,
        val reasonLabel: String?,
        val toolErrorCount: Int,
        val llmCalls: Int,
        val costUsd: BigDecimal,
        val latencyMs: Int,
    )

    @GetMapping("/members/{memberId}/logs")
    @RequiresAction(action = Actions.READ)
    fun memberLogs(
        @PathVariable memberId: Long,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") month: YearMonth?,
    ): ResponseEntity<ApiResponse<List<ChatLogRes>>> {
        val ym = month ?: YearMonth.now()
        val logs = chatLogRepository.findByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            memberId, ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay(),
        ).map {
            val tools = it.tools?.split(",")?.filter { t -> t.isNotBlank() }.orEmpty()
            val labels = ChatLogLabel.of(it.route, it.reason, tools)
            ChatLogRes(
                id = it.id, createdAt = it.createdAt, route = it.route, reason = it.reason, tools = tools,
                routeLabel = labels.routeLabel, requestLabel = labels.requestLabel, reasonLabel = labels.reasonLabel,
                toolErrorCount = it.toolErrorCount.toInt(), llmCalls = it.llmCalls.toInt(),
                costUsd = it.costUsd, latencyMs = it.latencyMs,
            )
        }
        return ResponseEntity(ApiResponse(code = ResponseCode.ASSISTANT_USAGE_LOG_GET.code, message = ResponseCode.ASSISTANT_USAGE_LOG_GET.message, result = logs), HttpStatus.OK)
    }

    private fun sum(logs: List<AssistantChatLog>) = UsageSum(
        turns = logs.size,
        llmCalls = logs.sumOf { it.llmCalls.toInt() },
        inputTokens = logs.sumOf { it.inputTokens.toLong() },
        outputTokens = logs.sumOf { it.outputTokens.toLong() },
        cacheReadTokens = logs.sumOf { it.cacheReadTokens.toLong() },
        costUsd = logs.fold(BigDecimal.ZERO) { acc, l -> acc + l.costUsd },
        rejectCount = logs.count { it.route == "REJECT" },
        askCount = logs.count { it.route == "ASK_USER" },
        errorCount = logs.count { it.route == "ERROR" || it.route == "BLOCKED" },
        avgLatencyMs = if (logs.isEmpty()) 0 else logs.sumOf { it.latencyMs.toLong() }.div(logs.size).toInt(),
    )
}
