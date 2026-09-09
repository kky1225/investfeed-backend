package com.example.investfeed.domain.index.controller

import com.example.investfeed.kiwoom.socket.KiwoomStreamClient
import com.example.investfeed.kiwoom.socket.dto.StreamMarket
import com.example.investfeed.kiwoom.socket.dto.StreamEntry
import com.example.investfeed.domain.index.dto.req.IndexDetailReq
import com.example.investfeed.domain.index.dto.req.IndexStreamReq
import com.example.investfeed.domain.index.dto.res.IndexDetailRes
import com.example.investfeed.domain.index.dto.res.IndexListRes
import com.example.investfeed.domain.index.service.IndexService
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.common.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.STOCK_INDEX)
@RestController
@RequestMapping("/api/stock/indexes")
class IndexController(
    private val kiwoomStreamClient: KiwoomStreamClient,
    private val indexService: IndexService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
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

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamIndexes(
        @RequestBody req: IndexStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamIndexes: $req" }

        kiwoomStreamClient.register(
            StreamEntry(
                market = StreamMarket.KRX,
                items = req.items,
                types = listOf("0J")
            )
        )

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INDEX_STREAM.code,
                message = ResponseCode.INDEX_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("/{indsCd}")
    @RequiresAction(action = Actions.READ)
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
}
