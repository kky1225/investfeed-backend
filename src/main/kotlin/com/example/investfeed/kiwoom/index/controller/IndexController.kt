package com.example.investfeed.kiwoom.index.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.index.dto.req.IndexDetailReq
import com.example.investfeed.kiwoom.index.dto.res.IndexDetailRes
import com.example.investfeed.kiwoom.index.dto.res.IndexListRes
import com.example.investfeed.kiwoom.index.service.IndexService
import com.example.investfeed.kiwoom.sect.dto.socket.req.SectIndexListStream
import com.example.investfeed.kiwoom.sect.dto.socket.req.SectIndexListStreamReq
import com.example.investfeed.kiwoom.sect.service.SectSocketService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/index")
class IndexController(
    private val indexService: IndexService,
    private val SectSocketService: SectSocketService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("list")
    fun indexList(): ResponseEntity<ApiResponse<IndexListRes?>> {
        log.info { "indexList" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INDEX_LIST.code,
                message = ResponseCode.INDEX_LIST.message,
                result = indexService.indexList()
            ), HttpStatus.OK
        )
    }

    @GetMapping("/list/stream")
    fun indexListStream(): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "indexListStream" }

        SectSocketService.sectIndexListStream(
            req = SectIndexListStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    SectIndexListStream(
                        item = listOf("001", "101", "201"),
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

    @GetMapping("detail")
    fun indexDetail(
        req: IndexDetailReq
    ): ResponseEntity<ApiResponse<IndexDetailRes<*>?>> {
        log.info { "indexDetail" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INDEX_DETAIL.code,
                message = ResponseCode.INDEX_DETAIL.message,
                result = indexService.indexDetail(req = req)
            ), HttpStatus.OK
        )
    }
}