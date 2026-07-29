package com.example.investfeed.kiwoom.us.rank.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.us.rank.dto.req.*
import com.example.investfeed.kiwoom.us.rank.dto.res.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class UsRankClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}
    private final val RANK_URL = "/api/us/rkinfo"
    private final val STKINFO_URL = "/api/us/stkinfo"

    @KiwoomToken
    fun usStockTradeValueList(
        req: KiwoomUsStockTradeValueListReq
    ): KiwoomUsStockTradeValueListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + RANK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "usa20540")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomUsStockTradeValueListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw UsStockTradeValueListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: UsStockTradeValueListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "usStockTradeValueList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun usStockTradeVolumeList(
        req: KiwoomUsStockTradeVolumeListReq
    ): KiwoomUsStockTradeVolumeListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + RANK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "usa20530")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomUsStockTradeVolumeListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw UsStockTradeVolumeListException()
            }

            return res
        }catch(e: KiwoomApiException){
            throw e
        }catch(e: UsStockTradeVolumeListException){
            throw e
        }catch (e: Exception) {
            log.warn { "usStockTradeVolumeList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun usStockSurgeTradeVolumeList(
        req: KiwoomUsSurgeTradeVolumeListReq
    ): KiwoomUsSurgeTradeVolumeListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + STKINFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "usa20520")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomUsSurgeTradeVolumeListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw UsSurgeTradeVolumeListException()
            }

            return res
        }catch(e: KiwoomApiException){
            throw e
        }catch(e: UsSurgeTradeVolumeListException){
            throw e
        }catch (e: Exception) {
            log.warn { "usStockSurgeTradeVolumeList Error" }

            throw RuntimeException(e.message)
        }
    }
}
