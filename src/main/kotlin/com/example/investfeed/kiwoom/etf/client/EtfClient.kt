package com.example.investfeed.kiwoom.etf.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.etf.dto.req.KiwoomEtfTimeTrendReq
import com.example.investfeed.kiwoom.etf.dto.res.KiwoomEtfTimeTrendRes
import com.example.investfeed.kiwoom.exception.EtfTimeTrendException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.support.withQuotaRetry
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import kotlin.coroutines.cancellation.CancellationException

@Component
class EtfClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}
    private final val ETF_URL = "/api/dostk/etf"

    @KiwoomToken
    suspend fun etfTimeTrend(
        req: KiwoomEtfTimeTrendReq
    ): KiwoomEtfTimeTrendRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = withQuotaRetry("ka40006", { it.return_code }) { kiwoomWebClient.post()
                .uri(DEFAULT_URL + ETF_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka40006")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomEtfTimeTrendRes>() }

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=etfTimeTrend, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw EtfTimeTrendException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: EtfTimeTrendException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "etfTimeTrend Error" }

            throw RuntimeException(e.message)
        }
    }
}
