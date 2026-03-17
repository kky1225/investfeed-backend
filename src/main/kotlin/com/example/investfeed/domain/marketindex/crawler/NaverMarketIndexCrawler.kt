package com.example.investfeed.domain.marketindex.crawler

import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NaverMarketIndexCrawler {

    private val log = KotlinLogging.logger {}

    companion object {
        private const val TARGET_URL = "https://stock.naver.com/market/stock/usa"

        private val TARGET_NAMES = mapOf(
            "나스닥 종합" to MarketIndexType.NASDAQ,
            "S&P 500" to MarketIndexType.SP500,
            "VIX" to MarketIndexType.VIX,
            "필라델피아 반도체" to MarketIndexType.PHILADELPHIA_SEMICONDUCTOR,
            "미국 USD" to MarketIndexType.USD_KRW,
            "달러인덱스" to MarketIndexType.DOLLAR_INDEX,
            "국제 금" to MarketIndexType.GOLD_INTERNATIONAL,
            "WTI" to MarketIndexType.WTI,
            "코스피" to MarketIndexType.KOSPI,
            "코스닥" to MarketIndexType.KOSDAQ,
        )
    }

    fun crawl(): List<MarketIndexRes> {
        Playwright.create().use { playwright ->
            val browser = playwright.chromium().launch(
                BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(listOf(
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-gpu",
                    ))
            )

            browser.use {
                val page = browser.newPage(
                    Browser.NewPageOptions().setUserAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
                    )
                )

                return extractMarketIndexList(page)
            }
        }
    }

    private fun extractMarketIndexList(page: Page): List<MarketIndexRes> {
        page.navigate(TARGET_URL, Page.NavigateOptions().setTimeout(60_000.0))
        page.waitForLoadState(LoadState.LOAD)
        page.waitForTimeout(5000.0)

        log.info { "실제 URL: ${page.url()}" }

        // 렌더링된 텍스트를 가져와 Kotlin에서 직접 파싱
        val bodyText = page.evaluate("() => document.body.innerText") as? String ?: ""

        if (bodyText.isBlank()) {
            log.warn { "body innerText가 비어있습니다" }
            return emptyList()
        }

        val lines = bodyText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        log.debug { "전체 라인 수: ${lines.size}" }

        return parseLines(lines)
    }

    private fun parseLines(lines: List<String>): List<MarketIndexRes> {
        val result = mutableListOf<MarketIndexRes>()

        TARGET_NAMES.forEach { (name, type) ->
            val nameIdx = lines.indexOfFirst { it == name }
            if (nameIdx == -1) {
                log.warn { "[$name] innerText에서 항목을 찾지 못했습니다" }
                return@forEach
            }

            val chunk = lines.drop(nameIdx + 1).take(10)
            log.debug { "[$name] 파싱 청크: $chunk" }

            val parsed = parseChunk(name, chunk) ?: return@forEach
            result.add(
                MarketIndexRes(
                    type = type.name,
                    name = name,
                    price = parsed.price,
                    changeAmount = parsed.changeAmount,
                    changeRate = parsed.changeRate,
                    delayStatus = parsed.delayStatus,
                    updatedAt = LocalDateTime.now(),
                )
            )
            log.info { "[$name] price=${parsed.price} changeAmount=${parsed.changeAmount} changeRate=${parsed.changeRate} delayStatus=${parsed.delayStatus}" }
        }

        return result
    }

    private fun parseChunk(name: String, chunk: List<String>): ParsedIndex? {
        val delayStatus = chunk.firstOrNull { it == "실시간" || it.contains("지연") || it == "장마감" } ?: "실시간"

        val numberPattern = Regex("^[0-9,]+\\.?[0-9]*$")
        val priceCandidate = chunk.filter { it.matches(numberPattern) }

        val price = priceCandidate.getOrNull(0)
        val changeAmountRaw = priceCandidate.getOrNull(1) ?: ""

        if (price == null) {
            log.warn { "[$name] price 파싱 실패. chunk=$chunk" }
            return null
        }

        val direction = chunk.firstOrNull { it == "상승" || it == "하락" || it == "보합" }
        val sign = when (direction) {
            "상승" -> "+"
            "하락" -> "-"
            else -> ""
        }

        val changeAmount = if (changeAmountRaw.isNotBlank()) "$sign$changeAmountRaw" else ""

        val changeRate = chunk
            .firstOrNull { it.contains("%") }
            ?.replace("(", "")
            ?.replace(")", "")
            ?: ""

        return ParsedIndex(
            price = price,
            changeAmount = changeAmount,
            changeRate = changeRate,
            delayStatus = delayStatus,
        )
    }

    private data class ParsedIndex(
        val price: String,
        val changeAmount: String,
        val changeRate: String,
        val delayStatus: String,
    )
}
