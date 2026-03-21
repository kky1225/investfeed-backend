package com.example.investfeed.domain.marketindex.crawler

import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Component
class NaverMarketIndexCrawler(
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    private val webClient = WebClient.builder()
        .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) }
        .build()

    companion object {
        private val WORLD_INDEX_API = mapOf(
            MarketIndexType.NASDAQ to "https://api.stock.naver.com/index/.IXIC/basic",
            MarketIndexType.SP500 to "https://api.stock.naver.com/index/.INX/basic",
            MarketIndexType.VIX to "https://api.stock.naver.com/index/.VIX/basic",
            MarketIndexType.PHILADELPHIA_SEMICONDUCTOR to "https://api.stock.naver.com/index/.SOX/basic",
        )

        private val DOMESTIC_INDEX_API = mapOf(
            MarketIndexType.KOSPI to "https://m.stock.naver.com/api/index/KOSPI/basic",
            MarketIndexType.KOSDAQ to "https://m.stock.naver.com/api/index/KOSDAQ/basic",
        )

        private const val MARKET_INDEX_URL = "https://finance.naver.com/marketindex/"
        private val MARKET_INDEX_CODE_MAP = mapOf(
            "FX_USDKRW" to MarketIndexType.USD_KRW,
            "FX_USDX" to MarketIndexType.DOLLAR_INDEX,
            "OIL_CL" to MarketIndexType.WTI,
            "CMDT_GC" to MarketIndexType.GOLD_INTERNATIONAL,
        )
    }

    fun crawl(): List<MarketIndexRes> {
        val result = mutableListOf<MarketIndexRes>()
        val now = LocalDateTime.now()

        val allApiMap = WORLD_INDEX_API + DOMESTIC_INDEX_API
        val monos = allApiMap.map { (type, url) ->
            webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono<String>()
                .map { body -> parseJsonIndex(type, body, now) }
                .onErrorResume { e ->
                    log.error(e) { "[${type.displayName}] API 호출 실패: $url" }
                    Mono.empty()
                }
        }

        val jsonResults = Mono.zip(monos) { results ->
            results.filterIsInstance<MarketIndexRes>()
        }.block() ?: emptyList()

        result.addAll(jsonResults)

        try {
            val htmlResults = fetchMarketIndexFromHtml(now)
            result.addAll(htmlResults)
        } catch (e: Exception) {
            log.error(e) { "환율/원자재 HTML 파싱 실패" }
        }

        return result
    }

    private fun parseJsonIndex(type: MarketIndexType, body: String, now: LocalDateTime): MarketIndexRes {
        val node = objectMapper.readTree(body)

        val price = node.textOrNull("closePrice") ?: ""
        val changeAmount = node.textOrNull("compareToPreviousClosePrice") ?: ""
        val fluctuationsRatio = node.textOrNull("fluctuationsRatio") ?: ""

        val direction = node.path("compareToPreviousPrice").textOrNull("name") ?: ""
        val sign = when (direction) {
            "RISING" -> "+"
            "FALLING" -> "-"
            else -> ""
        }

        val changeAmountWithSign = if (changeAmount.isNotBlank() && !changeAmount.startsWith("+") && !changeAmount.startsWith("-")) {
            "$sign$changeAmount"
        } else {
            changeAmount
        }

        val changeRateWithSign = if (fluctuationsRatio.isNotBlank() && !fluctuationsRatio.startsWith("+") && !fluctuationsRatio.startsWith("-")) {
            "$sign${fluctuationsRatio}%"
        } else if (fluctuationsRatio.isNotBlank()) {
            "${fluctuationsRatio}%"
        } else {
            ""
        }

        val delayTimeName = node.textOrNull("delayTimeName") ?: ""
        val marketStatus = node.textOrNull("marketStatus") ?: ""
        val delayStatus = when {
            marketStatus == "CLOSE" -> "장마감"
            delayTimeName.isNotBlank() -> delayTimeName
            else -> "실시간"
        }

        return MarketIndexRes(
            type = type.name,
            name = type.displayName,
            price = price,
            changeAmount = changeAmountWithSign,
            changeRate = changeRateWithSign,
            delayStatus = delayStatus,
            updatedAt = now,
        )
    }

    private fun fetchMarketIndexFromHtml(now: LocalDateTime): List<MarketIndexRes> {
        val html = webClient.get()
            .uri(MARKET_INDEX_URL)
            .retrieve()
            .bodyToMono<String>()
            .block() ?: return emptyList()

        val result = mutableListOf<MarketIndexRes>()
        val processedCodes = mutableSetOf<String>()

        val blocks = html.split("marketindexCd=")
        for (block in blocks.drop(1)) {
            val codeMatch = Regex("^([A-Z_]+)").find(block) ?: continue
            val code = codeMatch.groupValues[1]

            val type = MARKET_INDEX_CODE_MAP[code] ?: continue
            if (!processedCodes.add(code)) continue // 중복 블록 스킵

            val chunk = block.take(800)

            val price = Regex("""class="value">([\d,.]+)""").find(chunk)?.groupValues?.get(1) ?: continue
            val changeAmount = Regex("""class="change">\s*([\d,.]+)""").find(chunk)?.groupValues?.get(1) ?: "0"

            val isUp = chunk.contains("point_up")
            val isDown = chunk.contains("point_dn")
            val sign = when {
                isUp -> "+"
                isDown -> "-"
                else -> ""
            }

            val priceVal = price.replace(",", "").toDoubleOrNull() ?: 0.0
            val changeVal = changeAmount.replace(",", "").toDoubleOrNull() ?: 0.0
            val changeRate = if (priceVal > 0 && changeVal > 0) {
                val rate = (changeVal / priceVal) * 100
                "$sign%.2f%%".format(rate)
            } else {
                ""
            }

            result.add(
                MarketIndexRes(
                    type = type.name,
                    name = type.displayName,
                    price = price,
                    changeAmount = "$sign$changeAmount",
                    changeRate = changeRate,
                    delayStatus = "실시간",
                    updatedAt = now,
                )
            )
        }

        return result
    }

    private fun JsonNode.textOrNull(fieldName: String): String? {
        val node = this.get(fieldName) ?: return null
        return if (node.isTextual) node.asText() else node.toString()
    }
}
