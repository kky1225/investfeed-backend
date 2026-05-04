package com.example.investfeed.domain.crypto.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.crypto.dto.req.CryptoDetailReq
import com.example.investfeed.domain.crypto.dto.req.CryptoSearchReq
import com.example.investfeed.domain.crypto.dto.req.CryptoStreamReq
import com.example.investfeed.domain.crypto.dto.res.CryptoDetailRes
import com.example.investfeed.domain.crypto.dto.res.CryptoListRes
import com.example.investfeed.domain.crypto.dto.res.CryptoRankItem
import com.example.investfeed.domain.crypto.dto.res.CryptoSearchItem
import com.example.investfeed.domain.crypto.service.CryptoService
import com.example.investfeed.common.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.CRYPTO_PUBLIC)
@RestController
@RequestMapping("/api/cryptos")
class CryptoController(
    private val cryptoService: CryptoService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listCryptos(): ResponseEntity<ApiResponse<CryptoListRes>> {
        log.info { "listCryptos" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_LIST.code,
                message = ResponseCode.CRYPTO_LIST.message,
                result = cryptoService.listCryptos()
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamCryptos(
        @RequestBody req: CryptoStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamCryptos: $req" }

        cryptoService.streamCryptos(req.items)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_LIST_STREAM.code,
                message = ResponseCode.CRYPTO_LIST_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("/ranks")
    @RequiresAction(action = Actions.READ)
    fun listCryptoRanks(): ResponseEntity<ApiResponse<List<CryptoRankItem>>> {
        log.info { "listCryptoRanks" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_RANK_LIST.code,
                message = ResponseCode.CRYPTO_RANK_LIST.message,
                result = cryptoService.listCryptoRanks()
            ), HttpStatus.OK
        )
    }

    @GetMapping("/ranks/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamCryptoRanks(): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamCryptoRanks" }

        cryptoService.streamCryptoRanks()

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_RANK_STREAM.code,
                message = ResponseCode.CRYPTO_RANK_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("/search")
    @RequiresAction(action = Actions.READ)
    fun searchCryptos(
        req: CryptoSearchReq
    ): ResponseEntity<ApiResponse<List<CryptoSearchItem>>> {
        log.info { "searchCryptos: ${req.keyword}" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_SEARCH.code,
                message = ResponseCode.CRYPTO_SEARCH.message,
                result = cryptoService.searchCryptos(req.keyword)
            ), HttpStatus.OK
        )
    }

    @GetMapping("/{market}")
    @RequiresAction(action = Actions.READ)
    fun getCrypto(
        @PathVariable market: String,
        req: CryptoDetailReq
    ): ResponseEntity<ApiResponse<CryptoDetailRes>> {
        log.info { "getCrypto: market=$market, chartType=${req.chartType}" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_DETAIL.code,
                message = ResponseCode.CRYPTO_DETAIL.message,
                result = cryptoService.getCrypto(market = market, req = req)
            ), HttpStatus.OK
        )
    }
}
