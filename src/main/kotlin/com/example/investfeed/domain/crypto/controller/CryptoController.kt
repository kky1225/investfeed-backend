package com.example.investfeed.domain.crypto.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.crypto.dto.req.CryptoDetailReq
import com.example.investfeed.domain.crypto.dto.req.CryptoDetailStreamReq
import com.example.investfeed.domain.crypto.dto.req.CryptoSearchReq
import com.example.investfeed.domain.crypto.dto.res.CryptoDetailRes
import com.example.investfeed.domain.crypto.dto.res.CryptoListRes
import com.example.investfeed.domain.crypto.dto.res.CryptoSearchItem
import com.example.investfeed.domain.crypto.service.CryptoService
import com.example.investfeed.kiwoom.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/crypto")
class CryptoController(
    private val cryptoService: CryptoService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("list")
    fun cryptoList(): ResponseEntity<ApiResponse<CryptoListRes>> {
        log.info { "cryptoList" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_LIST.code,
                message = ResponseCode.CRYPTO_LIST.message,
                result = cryptoService.cryptoList()
            ), HttpStatus.OK
        )
    }

    @GetMapping("list/stream")
    fun cryptoListStream(): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "cryptoListStream" }

        cryptoService.cryptoListStream()

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_LIST_STREAM.code,
                message = ResponseCode.CRYPTO_LIST_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("detail/stream")
    fun cryptoDetailStream(
        req: CryptoDetailStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "cryptoDetailStream: ${req.market}" }

        cryptoService.cryptoDetailStream(req.market)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_DETAIL.code,
                message = ResponseCode.CRYPTO_DETAIL.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("detail")
    fun cryptoDetail(req: CryptoDetailReq): ResponseEntity<ApiResponse<CryptoDetailRes>> {
        log.info { "cryptoDetail: market=${req.market}, chartType=${req.chartType}" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_DETAIL.code,
                message = ResponseCode.CRYPTO_DETAIL.message,
                result = cryptoService.cryptoDetail(req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("search")
    fun cryptoSearch(
        req: CryptoSearchReq
    ): ResponseEntity<ApiResponse<List<CryptoSearchItem>>> {
        log.info { "cryptoSearch: ${req.keyword}" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_SEARCH.code,
                message = ResponseCode.CRYPTO_SEARCH.message,
                result = cryptoService.cryptoSearch(req.keyword)
            ), HttpStatus.OK
        )
    }

}
