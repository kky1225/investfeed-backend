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
@RequestMapping("/api/goals")
class InvestmentGoalController(
    private val investmentGoalService: InvestmentGoalService
) {

    @PostMapping
    fun createGoal(
        @Valid @RequestBody req: InvestmentGoalCreateReq
    ): ResponseEntity<ApiResponse<InvestmentGoalRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.GOAL_CREATE.code,
                message = ResponseCode.GOAL_CREATE.message,
                result = investmentGoalService.createGoal(req)
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/{id}")
    fun updateGoal(
        @PathVariable id: Long,
        @Valid @RequestBody req: InvestmentGoalUpdateReq
    ): ResponseEntity<ApiResponse<InvestmentGoalRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.GOAL_UPDATE.code,
                message = ResponseCode.GOAL_UPDATE.message,
                result = investmentGoalService.updateGoal(id, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("/{id}")
    fun deleteGoal(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        investmentGoalService.deleteGoal(id)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.GOAL_DELETE.code,
                message = ResponseCode.GOAL_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping
    fun listGoals(): ResponseEntity<ApiResponse<GoalDashboardRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.GOAL_LIST.code,
                message = ResponseCode.GOAL_LIST.message,
                result = investmentGoalService.listGoals()
            ), HttpStatus.OK
        )
    }

    @GetMapping("/dashboard")
    fun getGoalsDashboard(): ResponseEntity<ApiResponse<GoalDashboardRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.GOAL_LIST.code,
                message = ResponseCode.GOAL_LIST.message,
                result = investmentGoalService.getGoalsDashboard()
            ), HttpStatus.OK
        )
    }
}
