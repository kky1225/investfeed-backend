package com.example.investfeed.domain.index.controller

import com.example.investfeed.domain.index.dto.req.IndexDetailReq
import com.example.investfeed.domain.index.dto.res.IndexDetailRes
import com.example.investfeed.domain.index.dto.res.IndexListRes
import com.example.investfeed.domain.index.service.IndexService
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.kiwoom.realtime.client.RealTimeClient
import com.example.investfeed.kiwoom.realtime.dto.SectIndexListStream
import com.example.investfeed.kiwoom.realtime.dto.SectIndexListStreamReq
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stock/indexes")
class IndexController(
    private val indexService: IndexService,
    private val realTimeClient: RealTimeClient,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    fun listIndexes(): ResponseEntity<ApiResponse<IndexListRes?>> {
        log.info { "listIndexes" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INDEX_LIST.code,
                message = ResponseCode.INDEX_LIST.message,
                result = indexService.listIndexes()
            ), HttpStatus.OK
        )
    }

    @GetMapping("/stream")
    fun streamIndexes(): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamIndexes" }

        realTimeClient.sectIndexListStream(
            req = SectIndexListStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    SectIndexListStream(
                        item = listOf("001", "101", "201", "150"),
                        type = listOf("0J")
                    )
                )
            )
        )

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INDEX_WS_LIST.code,
                message = ResponseCode.INDEX_WS_LIST.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("/{indsCd}")
    fun getIndex(
        @PathVariable indsCd: String,
        req: IndexDetailReq
    ): ResponseEntity<ApiResponse<IndexDetailRes>> {
        log.info { "getIndex: indsCd=$indsCd, $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INDEX_DETAIL.code,
                message = ResponseCode.INDEX_DETAIL.message,
                result = indexService.getIndex(indsCd = indsCd, req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("/{indsCd}/stream")
    fun streamIndex(
        @PathVariable indsCd: String
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamIndex: indsCd=$indsCd" }

        realTimeClient.sectIndexListStream(
            req = SectIndexListStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    SectIndexListStream(
                        item = listOf(indsCd),
                        type = listOf("0J")
                    )
                )
            )
        )

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INDEX_WS_DETAIL.code,
                message = ResponseCode.INDEX_WS_DETAIL.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
