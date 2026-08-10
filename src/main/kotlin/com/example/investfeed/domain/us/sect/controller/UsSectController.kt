package com.example.investfeed.domain.us.sect.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.us.sect.dto.req.UsSectStockListReq
import com.example.investfeed.domain.us.sect.dto.req.UsSectStockStreamReq
import com.example.investfeed.domain.us.sect.dto.res.UsSectListRes
import com.example.investfeed.domain.us.sect.dto.res.UsSectStockListRes
import com.example.investfeed.domain.us.sect.service.UsSectService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.US_STOCK_SECT)
@RestController
@RequestMapping("/api/us-stock/sects")
class UsSectController(
    private val usSectService: UsSectService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listUsSects(): ResponseEntity<ApiResponse<UsSectListRes>> {
        log.info { "listUsSects" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_SECT_LIST.code,
                message = ResponseCode.US_SECT_LIST.message,
                result = usSectService.listUsSects()
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamUsSectStocks(
        @RequestBody req: UsSectStockStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamUsSectStocks: $req" }

        usSectService.streamUsStocks(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_SECT_STOCK_STREAM.code,
                message = ResponseCode.US_SECT_STOCK_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("/{indsCd}/stocks")
    @RequiresAction(action = Actions.READ)
    fun listStocksBySect(
        @PathVariable indsCd: String,
        req: UsSectStockListReq
    ): ResponseEntity<ApiResponse<UsSectStockListRes>> {
        log.info { "listStocksBySect : indsCd=$indsCd, $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_SECT_STOCK_LIST.code,
                message = ResponseCode.US_SECT_STOCK_LIST.message,
                result = usSectService.listStocksBySect(indsCd = indsCd, req = req)
            ), HttpStatus.OK
        )
    }
}
