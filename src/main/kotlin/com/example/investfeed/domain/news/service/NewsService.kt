package com.example.investfeed.domain.news.service

import com.example.investfeed.domain.news.dto.res.NewsItem
import com.example.investfeed.domain.news.dto.res.NewsListRes
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.naver.news.client.NaverNewsClient
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Service
class NewsService(
    private val naverNewsClient: NaverNewsClient,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}
    private val CACHE_PREFIX = RedisKeyPrefix.NEWS.prefix
    private val CACHE_TTL = 5L // 5분

    companion object {
        private val DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    }

    fun searchNews(query: String, page: Int = 1): NewsListRes {
        val cacheKey = "$CACHE_PREFIX${query}:$page"

        // Redis 캐시 조회
        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) {
            return try {
                objectMapper.readValue(cached, NewsListRes::class.java)
            } catch (e: Exception) {
                log.warn { "뉴스 캐시 파싱 실패: ${e.message}" }
                fetchAndCache(query, page, cacheKey)
            }
        }

        return fetchAndCache(query, page, cacheKey)
    }

    private fun fetchAndCache(query: String, page: Int, cacheKey: String): NewsListRes {
        val displayPerPage = 20
        val fetchSize = 100
        val start = (page - 1) * fetchSize + 1

        val dateItems = fetchTitleMatched(query, fetchSize, start, sort = "date")

        val merged = if (dateItems.size < displayPerPage) {
            val simItems = try {
                fetchTitleMatched(query, fetchSize, start, sort = "sim")
            } catch (e: Exception) {
                log.warn { "뉴스 정확도순 보충 조회 실패: ${e.message}" }
                emptyList()
            }
            val seenLinks = dateItems.map { it.link }.toMutableSet()
            (dateItems + simItems.filter { seenLinks.add(it.link) })
                .sortedByDescending { parsePubDate(it.pubDate) ?: ZonedDateTime.now().minusYears(100) }
        } else {
            dateItems
        }

        val items = merged
            .take(displayPerPage)
            .map { it.copy(pubDate = formatPubDate(it.pubDate)) }

        val result = NewsListRes(items = items, total = items.size)

        try {
            val json = objectMapper.writeValueAsString(result)
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL, TimeUnit.MINUTES)
        } catch (e: Exception) {
            log.warn { "뉴스 캐시 저장 실패: ${e.message}" }
        }

        return result
    }

    private fun parsePubDate(pubDate: String): ZonedDateTime? =
        try {
            ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
        } catch (e: Exception) {
            null
        }

    private fun formatPubDate(pubDate: String): String =
        parsePubDate(pubDate)?.format(DISPLAY_DATE_FORMAT) ?: pubDate

    private fun fetchTitleMatched(query: String, display: Int, start: Int, sort: String): List<NewsItem> {
        val naverRes = naverNewsClient.searchNews(query = query, display = display, start = start, sort = sort)

        return naverRes.items
            ?.map { item ->
                NewsItem(
                    title = stripHtml(item.title ?: ""),
                    link = item.originallink ?: item.link ?: "",
                    description = stripHtml(item.description ?: ""),
                    pubDate = item.pubDate ?: "",
                )
            }
            ?.filter { it.title.contains(query, ignoreCase = true) }
            ?: emptyList()
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
