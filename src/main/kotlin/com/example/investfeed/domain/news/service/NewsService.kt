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
        val cacheKey = "$CACHE_PREFIX${query}:$page"

        // Redis 캐시 조회
        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) {
            return try {
                objectMapper.readValue(cached, NewsListRes::class.java)
            } catch (e: Exception) {
                log.error { "뉴스 캐시 파싱 실패: ${e.message}" }
                fetchAndCache(query, page, cacheKey)
            }
        }

        return fetchAndCache(query, page, cacheKey)
    }

    private fun fetchAndCache(query: String, page: Int, cacheKey: String): NewsListRes {
        val displayPerPage = 20
        // 제목 필터링 후 결과가 줄어들므로 넉넉하게 조회
        val fetchSize = 100
        val start = (page - 1) * fetchSize + 1

        val naverRes = naverNewsClient.searchNews(query = query, display = fetchSize, start = start)

        val items = naverRes?.items
            ?.map { item ->
                NewsItem(
                    title = stripHtml(item.title ?: ""),
                    link = item.originallink ?: item.link ?: "",
                    description = stripHtml(item.description ?: ""),
                    pubDate = item.pubDate ?: "",
                )
            }
            ?.filter { it.title.contains(query, ignoreCase = true) }
            ?.take(displayPerPage)
            ?: emptyList()

        val result = NewsListRes(items = items, total = items.size)

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
