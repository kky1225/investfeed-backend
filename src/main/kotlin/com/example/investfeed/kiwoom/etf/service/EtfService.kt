package com.example.investfeed.kiwoom.etf.service

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.etf.dto.req.EtfPriceListReq
import com.example.investfeed.kiwoom.etf.dto.req.EtfInfoReq
import com.example.investfeed.kiwoom.etf.dto.req.EtfTradeDailyListReq
import com.example.investfeed.kiwoom.etf.dto.res.EtfPriceListRes
import com.example.investfeed.kiwoom.etf.dto.res.EtfInfoRes
import com.example.investfeed.kiwoom.etf.dto.res.EtfTradeDailyListRes
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.EtfInfoException
import com.example.investfeed.kiwoom.exception.EtfTradeDailyListException
import com.example.investfeed.kiwoom.exception.EtfPriceListException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class EtfService(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun etfPriceList(
        req: EtfPriceListReq
    ): EtfPriceListRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/etf")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka40004")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(EtfPriceListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw EtfPriceListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: EtfPriceListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "etfPriceList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun etfInfo(
        req: EtfInfoReq
    ): EtfInfoRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/etf")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka40002")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(EtfInfoRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw EtfInfoException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: EtfInfoException) {
            throw e
        }catch (e: Exception) {
            log.warn { "etfInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun etfTradeDailyList(
        req: EtfTradeDailyListReq
    ): EtfTradeDailyListRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/etf")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka40008")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(EtfTradeDailyListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw EtfTradeDailyListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: EtfTradeDailyListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "etfTradeDaily Error" }

            throw RuntimeException(e.message)
        }
    }
}