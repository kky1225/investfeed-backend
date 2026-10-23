package com.example.investfeed.pressrelease.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.pressrelease.dto.res.PressReleaseItem
import com.example.investfeed.pressrelease.exception.PressReleaseApiException
import com.example.investfeed.pressrelease.exception.PressReleaseResponseException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.HtmlUtils
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 공식 보도자료 RSS. 정식 API 가 아니라 필드 명세·변경 공지가 없으므로 호출부는 해석 실패를 "미발표"로 다룬다.
 */
@Service
class PressReleaseClient(
    @Qualifier("pressReleaseWebClient")
    private val pressReleaseWebClient: WebClient,
    @param:Value("\${press-release.bok-rss-url}")
    private val bokRssUrl: String,
    @param:Value("\${press-release.fed-monetary-rss-url}")
    private val fedMonetaryRssUrl: String,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val FED_HOST = "https://www.federalreserve.gov/"
        private val SCRIPT_STYLE = Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val TAG = Regex("<[^>]+>")
        private val WS = Regex("[\\s\\u00A0]+")
    }

    /** 한국은행 보도자료(전체) — 통화정책방향 결정문, GDP 속보·잠정 보도자료 포함. 최근 100건 */
    fun getBokPressReleases(): List<PressReleaseItem> = parseRss(fetch(bokRssUrl, "BOK RSS"))

    /** 연준 Monetary Policy 보도자료 — FOMC 성명은 제목만 오고 본문은 link 페이지에 있다 */
    fun getFedMonetaryReleases(): List<PressReleaseItem> = parseRss(fetch(fedMonetaryRssUrl, "Fed RSS"))

    /** 연준 보도자료 페이지 본문 텍스트. 피드의 link 를 따라가므로 연준 도메인만 허용한다 */
    fun getFedPageText(url: String): String {
        require(url.startsWith(FED_HOST)) { "연준 도메인이 아닌 링크입니다: $url" }
        return htmlToText(fetch(url, "Fed page"))
    }

    private fun fetch(url: String, label: String): String {
        try {
            val res = pressReleaseWebClient.get()
                .uri(url)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError(label); throw PressReleaseApiException() })
                .bodyToMono<String>()
                .block()

            if (res.isNullOrBlank()) {
                throw PressReleaseResponseException()
            }

            log.info { "PressRelease fetch: $label" }

            return res
        } catch (e: PressReleaseApiException) {
            throw e
        } catch (e: PressReleaseResponseException) {
            throw e
        } catch (e: Exception) {
            log.error { "PressRelease fetch Error ($label): ${e.message}" }

            throw RuntimeException(e.message)
        }
    }

    private fun parseRss(xml: String): List<PressReleaseItem> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            isExpandEntityReferences = false
        }
        val items = factory.newDocumentBuilder().parse(InputSource(StringReader(xml))).getElementsByTagName("item")
        return (0 until items.length).mapNotNull { i ->
            val item = items.item(i) as? Element ?: return@mapNotNull null
            fun text(tag: String) = item.getElementsByTagName(tag).item(0)?.textContent?.trim()
            val publishedAt = text("pubDate")
                ?.let { runCatching { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME) }.getOrNull() }
                ?: return@mapNotNull null
            PressReleaseItem(
                title = HtmlUtils.htmlUnescape(text("title") ?: return@mapNotNull null),
                link = text("link").orEmpty(),
                description = htmlToText(text("description").orEmpty()),
                publishedAt = publishedAt,
            )
        }
    }

    private fun htmlToText(raw: String): String =
        HtmlUtils.htmlUnescape(
            HtmlUtils.htmlUnescape(raw).replace(SCRIPT_STYLE, " ").replace(TAG, " ")
        ).replace(WS, " ").trim()
}
