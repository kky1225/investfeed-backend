package com.example.investfeed.domain.papertrade.admin.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.papertrade.admin.dto.req.AdminHoldingGradeReq
import com.example.investfeed.domain.papertrade.admin.dto.req.AdminPaperRealizedPnlReq
import com.example.investfeed.domain.papertrade.admin.dto.req.AdminPaperTradeHistoryReq
import com.example.investfeed.domain.papertrade.admin.dto.res.AdminHoldingGradeRes
import com.example.investfeed.domain.papertrade.admin.dto.res.AdminPaperAccountRes
import com.example.investfeed.domain.papertrade.admin.dto.res.AdminPaperRealizedPnlRes
import com.example.investfeed.domain.papertrade.admin.dto.res.AdminPaperTradeHistoryRes
import com.example.investfeed.domain.papertrade.admin.service.AdminPaperTradeService
import com.example.investfeed.domain.papertrade.dto.res.PaperTradeReportRes
import com.example.investfeed.domain.papertrade.service.PaperTradeReportService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.ADMIN_PAPER_TRADE)
@RestController
@RequestMapping("/api/admin/paper-trade")
class AdminPaperTradeController(
    private val adminPaperTradeService: AdminPaperTradeService,
    private val paperTradeReportService: PaperTradeReportService,
) {
    @GetMapping("/account")
    @RequiresAction(action = Actions.READ)
    fun account(): ResponseEntity<ApiResponse<AdminPaperAccountRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ADMIN_PAPER_TRADE_ACCOUNT.code,
                message = ResponseCode.ADMIN_PAPER_TRADE_ACCOUNT.message,
                result = adminPaperTradeService.getAccount(),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/realized-pnl")
    @RequiresAction(action = Actions.READ)
    fun realizedPnl(
        @ModelAttribute req: AdminPaperRealizedPnlReq
    ): ResponseEntity<ApiResponse<AdminPaperRealizedPnlRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ADMIN_PAPER_TRADE_REALIZED_PNL.code,
                message = ResponseCode.ADMIN_PAPER_TRADE_REALIZED_PNL.message,
                result = adminPaperTradeService.getRealizedPnl(req),
            ), HttpStatus.OK
        )
    }

    /** 거래내역 (kt00007 모의) — 단일 일자 조회. ordDt 미지정 시 오늘. */
    @GetMapping("/trade-history")
    @RequiresAction(action = Actions.READ)
    fun tradeHistory(
        @ModelAttribute req: AdminPaperTradeHistoryReq
    ): ResponseEntity<ApiResponse<AdminPaperTradeHistoryRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ADMIN_PAPER_TRADE_TRADE_HISTORY.code,
                message = ResponseCode.ADMIN_PAPER_TRADE_TRADE_HISTORY.message,
                result = adminPaperTradeService.getTradeHistory(req),
            ), HttpStatus.OK
        )
    }

    /** 모의매매 성과 리포트 — NAV vs 지수 verdict + 등급별 백테스트 신호품질. */
    @GetMapping("/report")
    @RequiresAction(action = Actions.READ)
    fun report(): ResponseEntity<ApiResponse<PaperTradeReportRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ADMIN_PAPER_TRADE_REPORT.code,
                message = ResponseCode.ADMIN_PAPER_TRADE_REPORT.message,
                result = paperTradeReportService.buildReport(),
            ), HttpStatus.OK
        )
    }

    /**
     * 보유 평가 — 22:10 HoldingGradeScheduler 산출 결과 (evalDate 단위).
     * evalDate 미지정 시 가장 최근 평가일 사용. 다음 거래일 09:00 매매 결정용 등급.
     */
    @GetMapping("/holding-grade")
    @RequiresAction(action = Actions.READ)
    fun holdingGrade(
        @ModelAttribute req: AdminHoldingGradeReq
    ): ResponseEntity<ApiResponse<AdminHoldingGradeRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ADMIN_PAPER_TRADE_HOLDING_GRADE.code,
                message = ResponseCode.ADMIN_PAPER_TRADE_HOLDING_GRADE.message,
                result = adminPaperTradeService.getHoldingGrade(req),
            ), HttpStatus.OK
        )
    }
}
