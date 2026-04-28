package com.example.investfeed.domain.calendar.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.calendar.dto.req.CalendarEventsReq
import com.example.investfeed.domain.calendar.dto.req.IndicatorHistoryReq
import com.example.investfeed.domain.calendar.dto.req.ManualCalendarEventReq
import com.example.investfeed.domain.calendar.dto.res.*
import com.example.investfeed.domain.calendar.service.EconomicCalendarService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/calendar")
class EconomicCalendarController(
    private val economicCalendarService: EconomicCalendarService,
) {

    @GetMapping("indicators")
    fun listIndicators(): ResponseEntity<ApiResponse<EconomicIndicatorsRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ECONOMIC_INDICATORS.code,
                message = ResponseCode.ECONOMIC_INDICATORS.message,
                result = economicCalendarService.listIndicators()
            ), HttpStatus.OK
        )
    }

    @GetMapping("history")
    fun getIndicatorHistory(
        @Valid @ModelAttribute req: IndicatorHistoryReq
    ): ResponseEntity<ApiResponse<IndicatorHistoryRes?>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ECONOMIC_HISTORY.code,
                message = ResponseCode.ECONOMIC_HISTORY.message,
                result = economicCalendarService.getIndicatorHistory(req.code, req.country)
            ), HttpStatus.OK
        )
    }

    @GetMapping("events")
    fun listEvents(
        @ModelAttribute req: CalendarEventsReq
    ): ResponseEntity<ApiResponse<CalendarEventsRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CALENDAR_EVENT_LIST.code,
                message = ResponseCode.CALENDAR_EVENT_LIST.message,
                result = economicCalendarService.listEvents(req.year, req.month)
            ), HttpStatus.OK
        )
    }

    @GetMapping("events/manual")
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

    @PostMapping("events")
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

    @PutMapping("events/{id}")
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

    @DeleteMapping("events/{id}")
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

    @PatchMapping("events")
    fun refreshEvents(
        @RequestBody req: CalendarEventsReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        economicCalendarService.refreshEvents(req.year, req.month)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CALENDAR_REFRESH.code,
                message = ResponseCode.CALENDAR_REFRESH.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
