package com.example.investfeed.naver.news.client

import com.example.investfeed.common.exception.ExternalApiFailureException
import com.example.investfeed.common.util.apiFailure
import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.naver.news.dto.res.NaverNewsRes
import com.example.investfeed.naver.news.exception.NaverNewsApiException
import com.example.investfeed.naver.news.exception.NaverNewsSearchException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class NaverNewsClient(
    @param:Value("\${naver-news.client-id}")
    private val clientId: String,
    @param:Value("\${naver-news.client-secret}")
    private val clientSecret: String,
    @Qualifier("naverNewsWebClient")
    private val webClient: WebClient,
) {
    private val log = KotlinLogging.logger {}

    fun searchNews(query: String, display: Int = 20, start: Int = 1, sort: String = "date"): NaverNewsRes {
        try {
            val res = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/v1/search/news.json")
                        .queryParam("query", query)
                        .queryParam("display", display)
                        .queryParam("start", start)
                        .queryParam("sort", sort)
                        .build()
                }
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("네이버 뉴스"); throw NaverNewsApiException() })
                .bodyToMono<NaverNewsRes>()
                .block()

            if (res?.items == null) {
                log.error { "네이버 뉴스 API 응답 오류: query=$query" }
                throw NaverNewsSearchException()
            }

            return res
        } catch (e: NaverNewsApiException) {
            throw e
        } catch (e: NaverNewsSearchException) {
            throw e
        } catch (e: Exception) {
            log.apiFailure(e) { "searchNews Error: query=$query" }

            throw ExternalApiFailureException(e)
        }
    }
}
