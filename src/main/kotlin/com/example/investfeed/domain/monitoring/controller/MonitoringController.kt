package com.example.investfeed.domain.monitoring.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.monitoring.dto.req.AcknowledgeLogReq
import com.example.investfeed.domain.monitoring.dto.req.BulkAcknowledgeReq
import com.example.investfeed.domain.monitoring.dto.req.ErrorLogsReq
import com.example.investfeed.domain.monitoring.dto.req.RedisInvalidateReq
import com.example.investfeed.domain.monitoring.dto.req.SchedulerConfigLogsReq
import com.example.investfeed.domain.monitoring.dto.req.SchedulerLogsReq
import com.example.investfeed.domain.monitoring.dto.req.UpdateSchedulerTimeoutReq
import com.example.investfeed.domain.monitoring.dto.res.*
import com.example.investfeed.domain.monitoring.entity.AckSourceType
import com.example.investfeed.domain.monitoring.service.ManualTriggerService
import com.example.investfeed.domain.monitoring.service.MonitoringService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/monitoring")
class MonitoringController(
    private val monitoringService: MonitoringService,
    private val manualTriggerService: ManualTriggerService,
) {
    @GetMapping("/scheduler")
    @PreAuthorize("hasRole('ADMIN')")
    fun scheduler(
        @ModelAttribute req: SchedulerLogsReq,
    ): ResponseEntity<ApiResponse<SchedulerOverviewRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_SCHEDULER_OVERVIEW.code,
                message = ResponseCode.MONITORING_SCHEDULER_OVERVIEW.message,
                result = monitoringService.getSchedulerOverview(req),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/config-logs")
    @PreAuthorize("hasRole('ADMIN')")
    fun configLogs(
        @ModelAttribute req: SchedulerConfigLogsReq,
    ): ResponseEntity<ApiResponse<ConfigLogsOverviewRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_CONFIG_LOGS_OVERVIEW.code,
                message = ResponseCode.MONITORING_CONFIG_LOGS_OVERVIEW.message,
                result = monitoringService.getConfigLogsOverview(req),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/redis")
    @PreAuthorize("hasRole('ADMIN')")
    fun redis(): ResponseEntity<ApiResponse<RedisOverviewRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_REDIS_OVERVIEW.code,
                message = ResponseCode.MONITORING_REDIS_OVERVIEW.message,
                result = monitoringService.getRedisOverview(),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/errors")
    @PreAuthorize("hasRole('ADMIN')")
    fun errors(
        @ModelAttribute req: ErrorLogsReq,
    ): ResponseEntity<ApiResponse<ErrorLogsOverviewRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_ERROR_LOGS_OVERVIEW.code,
                message = ResponseCode.MONITORING_ERROR_LOGS_OVERVIEW.message,
                result = monitoringService.getErrorLogsOverview(req),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/api-calls")
    @PreAuthorize("hasRole('ADMIN')")
    fun apiCalls(): ResponseEntity<ApiResponse<ApiCallsOverviewRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_API_CALLS_OVERVIEW.code,
                message = ResponseCode.MONITORING_API_CALLS_OVERVIEW.message,
                result = monitoringService.getApiCallsOverview(),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    fun system(): ResponseEntity<ApiResponse<SystemOverviewRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_SYSTEM_OVERVIEW.code,
                message = ResponseCode.MONITORING_SYSTEM_OVERVIEW.message,
                result = monitoringService.getSystemOverview(),
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/scheduler-status/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateSchedulerTimeout(
        @PathVariable name: String,
        @Valid @RequestBody req: UpdateSchedulerTimeoutReq,
    ): ResponseEntity<ApiResponse<SchedulerStatusRes>> {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        val changedBy = userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_SCHEDULER_TIMEOUT_UPDATE.code,
                message = ResponseCode.MONITORING_SCHEDULER_TIMEOUT_UPDATE.message,
                result = monitoringService.updateTimeout(name, req, changedBy),
            ), HttpStatus.OK
        )
    }

    @PostMapping("/scheduler-status/{name}/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    fun triggerScheduler(@PathVariable name: String): ResponseEntity<ApiResponse<TriggerSchedulerRes>> {
        manualTriggerService.trigger(name)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_SCHEDULER_TRIGGER.code,
                message = ResponseCode.MONITORING_SCHEDULER_TRIGGER.message,
                result = TriggerSchedulerRes(schedulerName = name),
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/scheduler-logs/{id}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    fun acknowledgeLog(
        @PathVariable id: Long,
        @Valid @RequestBody req: AcknowledgeLogReq,
    ): ResponseEntity<ApiResponse<SchedulerLogRes>> {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        val changedBy = userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_SCHEDULER_LOG_ACKNOWLEDGE.code,
                message = ResponseCode.MONITORING_SCHEDULER_LOG_ACKNOWLEDGE.message,
                result = monitoringService.acknowledgeLog(id, req, changedBy),
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("/scheduler-logs/{id}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    fun cancelAcknowledgeLog(@PathVariable id: Long): ResponseEntity<ApiResponse<SchedulerLogRes>> {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        val changedBy = userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_SCHEDULER_LOG_CANCEL_ACKNOWLEDGE.code,
                message = ResponseCode.MONITORING_SCHEDULER_LOG_CANCEL_ACKNOWLEDGE.message,
                result = monitoringService.cancelAcknowledgeLog(id, changedBy),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/scheduler-logs/{id}/ack-history")
    @PreAuthorize("hasRole('ADMIN')")
    fun schedulerLogAckHistory(@PathVariable id: Long): ResponseEntity<ApiResponse<List<LogAckHistoryRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_ACK_HISTORY.code,
                message = ResponseCode.MONITORING_ACK_HISTORY.message,
                result = monitoringService.getAckHistory(AckSourceType.SCHEDULER_LOG, id),
            ), HttpStatus.OK
        )
    }

    @PostMapping("/scheduler-logs/acknowledge-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    fun bulkAcknowledgeSchedulerLogs(
        @Valid @RequestBody req: BulkAcknowledgeReq,
    ): ResponseEntity<ApiResponse<BulkAcknowledgeRes>> {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        val changedBy = userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_BULK_ACKNOWLEDGE.code,
                message = ResponseCode.MONITORING_BULK_ACKNOWLEDGE.message,
                result = monitoringService.bulkAcknowledgeSchedulerLogs(req, changedBy),
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/error-logs/{id}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    fun acknowledgeErrorLog(
        @PathVariable id: Long,
        @Valid @RequestBody req: AcknowledgeLogReq,
    ): ResponseEntity<ApiResponse<ErrorLogRes>> {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        val changedBy = userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_ERROR_LOG_ACKNOWLEDGE.code,
                message = ResponseCode.MONITORING_ERROR_LOG_ACKNOWLEDGE.message,
                result = monitoringService.acknowledgeErrorLog(id, req, changedBy),
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("/error-logs/{id}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    fun cancelAcknowledgeErrorLog(@PathVariable id: Long): ResponseEntity<ApiResponse<ErrorLogRes>> {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        val changedBy = userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_ERROR_LOG_CANCEL_ACKNOWLEDGE.code,
                message = ResponseCode.MONITORING_ERROR_LOG_CANCEL_ACKNOWLEDGE.message,
                result = monitoringService.cancelAcknowledgeErrorLog(id, changedBy),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/error-logs/{id}/ack-history")
    @PreAuthorize("hasRole('ADMIN')")
    fun errorLogAckHistory(@PathVariable id: Long): ResponseEntity<ApiResponse<List<LogAckHistoryRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_ACK_HISTORY.code,
                message = ResponseCode.MONITORING_ACK_HISTORY.message,
                result = monitoringService.getAckHistory(AckSourceType.ERROR_LOG, id),
            ), HttpStatus.OK
        )
    }

    @PostMapping("/error-logs/acknowledge-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    fun bulkAcknowledgeErrorLogs(
        @Valid @RequestBody req: BulkAcknowledgeReq,
    ): ResponseEntity<ApiResponse<BulkAcknowledgeRes>> {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        val changedBy = userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_BULK_ACKNOWLEDGE.code,
                message = ResponseCode.MONITORING_BULK_ACKNOWLEDGE.message,
                result = monitoringService.bulkAcknowledgeErrorLogs(req, changedBy),
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("/redis/cache")
    @PreAuthorize("hasRole('ADMIN')")
    fun invalidate(@ModelAttribute req: RedisInvalidateReq): ResponseEntity<ApiResponse<RedisInvalidateRes>> {
        val deleted = monitoringService.invalidatePrefix(req.prefix)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MONITORING_REDIS_INVALIDATE.code,
                message = ResponseCode.MONITORING_REDIS_INVALIDATE.message,
                result = RedisInvalidateRes(deleted = deleted),
            ), HttpStatus.OK
        )
    }
}
