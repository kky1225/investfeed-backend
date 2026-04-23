package com.example.investfeed.feargreed.client

import com.example.investfeed.feargreed.dto.res.FearGreedApiRes
import com.example.investfeed.feargreed.exception.FearGreedApiException
import com.example.investfeed.feargreed.exception.FearGreedResponseException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class FearGreedClient(
    @Qualifier("fearGreedWebClient")
    private val fearGreedWebClient: WebClient,
) {
    private val log = KotlinLogging.logger {}

    fun getFearGreedIndex(limit: Int = 30): FearGreedApiRes {
        try {
            val res = fearGreedWebClient.get()
                .uri { it.path("/fng/").queryParam("limit", limit).build() }
                .retrieve()
                .onStatus({ it.isError }, { throw FearGreedApiException() })
                .bodyToMono<FearGreedApiRes>()
                .block()

            if (res?.data == null) {
                throw FearGreedResponseException()
            }

            log.info { "getFearGreedIndex: limit=$limit" }

            return res
        } catch (e: FearGreedApiException) {
            throw e
        } catch (e: FearGreedResponseException) {
            throw e
        } catch (e: Exception) {
            log.error { "getFearGreedIndex Error: ${e.message}" }

            throw RuntimeException(e.message)
        }
    }
}
