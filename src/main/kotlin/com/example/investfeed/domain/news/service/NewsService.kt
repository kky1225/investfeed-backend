package com.example.investfeed.domain.news.service

import com.example.investfeed.domain.news.dto.res.NewsItem
import com.example.investfeed.domain.news.dto.res.NewsListRes
import com.example.investfeed.naver.news.client.NaverNewsClient
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class NewsService(
    private val naverNewsClient: NaverNewsClient,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}
    private val CACHE_PREFIX = "NEWS:"
    private val CACHE_TTL = 30L // 30분

    fun searchNews(query: String, page: Int = 1): NewsListRes {
        val display = 20
        val start = (page - 1) * display + 1
        val cacheKey = "$CACHE_PREFIX${query}:$page"

        // Redis 캐시 조회
        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) {
            return try {
                objectMapper.readValue(cached, NewsListRes::class.java)
            } catch (e: Exception) {
                log.error { "뉴스 캐시 파싱 실패: ${e.message}" }
                fetchAndCache(query, display, start, cacheKey)
            }
        }

        return fetchAndCache(query, display, start, cacheKey)
    }

    private fun fetchAndCache(query: String, display: Int, start: Int, cacheKey: String): NewsListRes {
        val naverRes = naverNewsClient.searchNews(query = query, display = display, start = start)

        val items = naverRes?.items?.map { item ->
            NewsItem(
                title = stripHtml(item.title ?: ""),
                link = item.originallink ?: item.link ?: "",
                description = stripHtml(item.description ?: ""),
                pubDate = item.pubDate ?: "",
            )
        } ?: emptyList()

        val result = NewsListRes(items = items, total = naverRes?.total ?: 0)

        // Redis 캐시 저장
        try {
            val json = objectMapper.writeValueAsString(result)
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL, TimeUnit.MINUTES)
        } catch (e: Exception) {
            log.error { "뉴스 캐시 저장 실패: ${e.message}" }
        }

        return result
    }

    private fun stripHtml(text: String): String {
        return text.replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&apos;", "'")
    }
}
