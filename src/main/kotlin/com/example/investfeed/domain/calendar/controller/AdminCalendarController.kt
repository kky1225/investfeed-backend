package com.example.investfeed.domain.calendar.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.calendar.dto.req.BulkRefreshReq
import com.example.investfeed.domain.calendar.dto.req.CalendarEventsReq
import com.example.investfeed.domain.calendar.dto.req.ManualCalendarEventReq
import com.example.investfeed.domain.calendar.dto.res.CalendarEvent
import com.example.investfeed.domain.calendar.service.BulkRefreshStatus
import com.example.investfeed.domain.calendar.service.CalendarBulkRefreshService
import com.example.investfeed.domain.calendar.service.EconomicCalendarService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.ADMIN_CALENDAR)
@RestController
@RequestMapping("/api/admin/calendar")
class AdminCalendarController(
    private val bulkRefreshService: CalendarBulkRefreshService,
    private val economicCalendarService: EconomicCalendarService,
) {

    @PostMapping("/bulk-refresh")
    @RequiresAction(action = Actions.CREATE)
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
    @RequiresAction(action = Actions.READ)
    fun bulkRefreshStatus(): ResponseEntity<ApiResponse<BulkRefreshStatus>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CALENDAR_BULK_REFRESH_STATUS.code,
                message = ResponseCode.CALENDAR_BULK_REFRESH_STATUS.message,
                result = bulkRefreshService.getStatus(),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/events")
    @RequiresAction(action = Actions.READ)
    fun listManualEvents(
        @ModelAttribute req: CalendarEventsReq
    ): ResponseEntity<ApiResponse<List<CalendarEvent>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CALENDAR_EVENT_LIST.code,
                message = ResponseCode.CALENDAR_EVENT_LIST.message,
                result = economicCalendarService.listManualEvents(req.year)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/events")
    @RequiresAction(action = Actions.CREATE)
    fun createEvent(
        @Valid @RequestBody req: ManualCalendarEventReq
    ): ResponseEntity<ApiResponse<CalendarEvent>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CALENDAR_EVENT_CREATE.code,
                message = ResponseCode.CALENDAR_EVENT_CREATE.message,
                result = economicCalendarService.createEvent(req)
            ), HttpStatus.OK
        )
    }

    @PutMapping("/events/{id}")
    @RequiresAction(action = Actions.UPDATE)
    fun updateEvent(
        @PathVariable id: Long,
        @Valid @RequestBody req: ManualCalendarEventReq
    ): ResponseEntity<ApiResponse<CalendarEvent>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CALENDAR_EVENT_UPDATE.code,
                message = ResponseCode.CALENDAR_EVENT_UPDATE.message,
                result = economicCalendarService.updateEvent(id, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("/events/{id}")
    @RequiresAction(action = Actions.DELETE)
    fun deleteEvent(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        economicCalendarService.deleteEvent(id)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CALENDAR_EVENT_DELETE.code,
                message = ResponseCode.CALENDAR_EVENT_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
