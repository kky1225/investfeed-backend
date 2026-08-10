package com.example.investfeed.kiwoom.us.sect.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.us.sect.dto.req.*
import com.example.investfeed.kiwoom.us.sect.dto.res.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity

@Component
class UsSectClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}
    private final val SECT_URL = "/api/us/sect"

    companion object {
        private const val MAX_STOCK_SIZE = 100
    }

    @KiwoomToken
    fun usSectPerformanceList(
        req: KiwoomUsSectPerformanceListReq
    ): KiwoomUsSectPerformanceListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + SECT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "usa23000")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomUsSectPerformanceListRes>()
                .block()

            if (res?.return_code != 0) {
                throw UsSectPerformanceListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: UsSectPerformanceListException) {
            throw e
        } catch (e: Exception) {
            log.warn { "usSectPerformanceList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun usSectStockList(
        req: KiwoomUsSectStockListReq
    ): KiwoomUsSectStockListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val list = mutableListOf<KiwoomUsSectStockRes>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + SECT_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "usa23100")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ it.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomUsSectStockListRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
                    throw UsSectStockListException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.result_list?.forEach { list.add(it) }

                contYn = entity.headers.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers.getFirst("next-key") ?: ""

                if (contYn == "N" || list.size >= MAX_STOCK_SIZE) {
                    break
                }
            }

            return KiwoomUsSectStockListRes(
                return_code = returnCode,
                return_msg = returnMsg,
                result_list = list.take(MAX_STOCK_SIZE)
            )
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: UsSectStockListException) {
            throw e
        } catch (e: Exception) {
            log.warn { "usSectStockList Error" }

            throw RuntimeException(e.message)
        }
    }
}
