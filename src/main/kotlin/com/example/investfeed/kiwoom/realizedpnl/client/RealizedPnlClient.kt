package com.example.investfeed.kiwoom.realizedpnl.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.RealizedPnlException
import com.example.investfeed.kiwoom.realizedpnl.dto.req.KiwoomRealizedPnlReq
import com.example.investfeed.kiwoom.realizedpnl.dto.res.KiwoomRealizedPnlRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class RealizedPnlClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun realizedPnl(req: KiwoomRealizedPnlReq): KiwoomRealizedPnlRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri("$DEFAULT_URL/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10074")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomRealizedPnlRes>()
                .block()

            if (res?.return_code != 0) {
                throw RealizedPnlException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: RealizedPnlException) {
            throw e
        } catch (e: Exception) {
            log.error { "realizedPnl Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }
}
