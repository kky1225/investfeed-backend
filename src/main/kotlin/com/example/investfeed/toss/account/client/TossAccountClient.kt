package com.example.investfeed.toss.account.client

import com.example.investfeed.toss.account.dto.res.TossAccount
import com.example.investfeed.toss.account.dto.res.TossAccountListRes
import com.example.investfeed.toss.annotation.TossToken
import com.example.investfeed.toss.auth.service.TossAuthClient
import com.example.investfeed.toss.exception.TossAccountListException
import com.example.investfeed.toss.exception.TossApiException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class TossAccountClient(
    @param:Value("\${toss.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("tossWebClient")
    private val tossWebClient: WebClient,
    private val tossAuthClient: TossAuthClient,
) {
    private val log = KotlinLogging.logger {}

    @TossToken
    fun getAccounts(): List<TossAccount> {
        val accessToken = tossAuthClient.getCurrentAccessToken()

        try {
            val res = tossWebClient.get()
                .uri("$DEFAULT_URL/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .onStatus({ it.isError }, { throw TossApiException() })
                .bodyToMono<TossAccountListRes>()
                .block()

            return res?.result ?: throw TossAccountListException()
        } catch (e: TossApiException) {
            throw e
        } catch (e: TossAccountListException) {
            throw e
        } catch (e: Exception) {
            log.warn { "toss getAccounts Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }
}
