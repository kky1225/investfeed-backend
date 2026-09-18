package com.example.investfeed.internal.assistant

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/assistant/tools")
class InternalToolController(
    private val stockClient: StockClient,
    private val marketIndexService: MarketIndexService,
) {

    data class StockQuoteRes(val stkCd: String, val stkNm: String?, val curPrc: String?, val fluRt: String?, val predPre: String?, val basePric: String?)

    @GetMapping("get_stock_quote")
    fun getStockQuote(@RequestParam stkCd: String): ResponseEntity<ApiResponse<StockQuoteRes?>> {
        val code = stkCd.substringBefore("_")
        val info = runBlocking { stockClient.stockInterest(KiwoomStockInterestReq(stk_cd = code)) }.atn_stk_infr?.firstOrNull()
        val res = info?.let { StockQuoteRes(stkCd = code, stkNm = it.stk_nm, curPrc = it.cur_prc, fluRt = it.flu_rt, predPre = it.pred_pre, basePric = it.base_pric) }
        return ResponseEntity(ApiResponse(code = ResponseCode.ASSISTANT_TOOL_RESULT.code, message = ResponseCode.ASSISTANT_TOOL_RESULT.message, result = res), HttpStatus.OK)
    }

    @GetMapping("get_global_indexes")
    fun getGlobalIndexes(): ResponseEntity<ApiResponse<List<MarketIndexRes>>> =
        ResponseEntity(ApiResponse(code = ResponseCode.ASSISTANT_TOOL_RESULT.code, message = ResponseCode.ASSISTANT_TOOL_RESULT.message, result = marketIndexService.listMarketIndexes()), HttpStatus.OK)
}
