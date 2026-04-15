package com.example.investfeed.naver.news.client

import com.example.investfeed.global.config.WebClientHttpClientFactory
import com.example.investfeed.naver.news.dto.res.NaverNewsRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class NaverNewsClient(
    @param:Value("\${naver-news.client-id}")
    private val clientId: String,
    @param:Value("\${naver-news.client-secret}")
    private val clientSecret: String,
) {
    private val log = KotlinLogging.logger {}
    private val webClient = WebClient.builder()
        .clientConnector(ReactorClientHttpConnector(WebClientHttpClientFactory.createDefaultHttpClient()))
        .baseUrl("https://openapi.naver.com")
        .build()

    fun searchNews(query: String, display: Int = 20, start: Int = 1, sort: String = "date"): NaverNewsRes? {
        return try {
            webClient.get()
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
                .bodyToMono<NaverNewsRes>()
                .block()
        } catch (e: Exception) {
            log.error { "네이버 뉴스 API 호출 실패: ${e.message}" }
            null
        }
    }
}
