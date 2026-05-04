package com.example.investfeed.domain.calendar.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.calendar.dto.req.CalendarEventsReq
import com.example.investfeed.domain.calendar.dto.req.IndicatorHistoryReq
import com.example.investfeed.domain.calendar.dto.res.*
import com.example.investfeed.domain.calendar.service.EconomicCalendarService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.CALENDAR_VIEW)
@RestController
@RequestMapping("/api/calendar")
class EconomicCalendarController(
    private val economicCalendarService: EconomicCalendarService,
) {

    @GetMapping("indicators")
    @RequiresAction(action = Actions.READ)
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
    @RequiresAction(action = Actions.READ)
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
    @RequiresAction(action = Actions.READ)
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

    @PatchMapping("events")
    @RequiresAction(action = Actions.UPDATE)
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
