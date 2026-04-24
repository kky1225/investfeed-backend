package com.example.investfeed.domain.goal.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.goal.dto.req.InvestmentGoalCreateReq
import com.example.investfeed.domain.goal.dto.req.InvestmentGoalUpdateReq
import com.example.investfeed.domain.goal.dto.res.GoalDashboardRes
import com.example.investfeed.domain.goal.dto.res.InvestmentGoalRes
import com.example.investfeed.domain.goal.service.InvestmentGoalService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/goal")
class InvestmentGoalController(
    private val investmentGoalService: InvestmentGoalService
) {

    @PostMapping
    fun create(
        @Valid @RequestBody req: InvestmentGoalCreateReq
    ): ResponseEntity<ApiResponse<InvestmentGoalRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.GOAL_CREATE.code,
                message = ResponseCode.GOAL_CREATE.message,
                result = investmentGoalService.create(req)
            ), HttpStatus.OK
        )
    }

    @PatchMapping("{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: InvestmentGoalUpdateReq
    ): ResponseEntity<ApiResponse<InvestmentGoalRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.GOAL_UPDATE.code,
                message = ResponseCode.GOAL_UPDATE.message,
                result = investmentGoalService.update(id, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("{id}")
    fun delete(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        investmentGoalService.delete(id)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.GOAL_DELETE.code,
                message = ResponseCode.GOAL_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping
    fun list(): ResponseEntity<ApiResponse<GoalDashboardRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.GOAL_LIST.code,
                message = ResponseCode.GOAL_LIST.message,
                result = investmentGoalService.getGoals()
            ), HttpStatus.OK
        )
    }

    @GetMapping("dashboard")
    fun dashboard(): ResponseEntity<ApiResponse<GoalDashboardRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.GOAL_LIST.code,
                message = ResponseCode.GOAL_LIST.message,
                result = investmentGoalService.getDashboardGoals()
            ), HttpStatus.OK
        )
    }
}
