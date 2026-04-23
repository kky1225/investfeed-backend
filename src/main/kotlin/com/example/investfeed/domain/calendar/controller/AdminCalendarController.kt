package com.example.investfeed.domain.calendar.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.calendar.dto.req.BulkRefreshReq
import com.example.investfeed.domain.calendar.service.BulkRefreshStatus
import com.example.investfeed.domain.calendar.service.CalendarBulkRefreshService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/calendar")
class AdminCalendarController(
    private val bulkRefreshService: CalendarBulkRefreshService,
) {

    @PostMapping("/bulk-refresh")
    @PreAuthorize("hasRole('ADMIN')")
    fun startBulkRefresh(@Valid @RequestBody req: BulkRefreshReq): ResponseEntity<ApiResponse<BulkRefreshStatus>> {
        bulkRefreshService.start(req.yearFrom, req.yearTo)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CALENDAR_BULK_REFRESH_START.code,
                message = ResponseCode.CALENDAR_BULK_REFRESH_START.message,
                result = bulkRefreshService.getStatus(),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/bulk-refresh/status")
    @PreAuthorize("hasRole('ADMIN')")
    fun bulkRefreshStatus(): ResponseEntity<ApiResponse<BulkRefreshStatus>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CALENDAR_BULK_REFRESH_STATUS.code,
                message = ResponseCode.CALENDAR_BULK_REFRESH_STATUS.message,
                result = bulkRefreshService.getStatus(),
            ), HttpStatus.OK
        )
    }
}
