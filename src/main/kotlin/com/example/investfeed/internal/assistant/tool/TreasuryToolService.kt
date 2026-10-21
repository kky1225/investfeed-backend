package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.assistant.dto.factsheet.KrTreasuryFact
import com.example.investfeed.domain.assistant.dto.factsheet.TreasuryFact
import com.example.investfeed.domain.assistant.dto.factsheet.YieldFact
import com.example.investfeed.domain.assistant.service.BriefingTemplateRenderer
import com.example.investfeed.domain.assistant.service.KrTreasuryFallbackService
import com.example.investfeed.domain.assistant.service.MarketFactSheetService
import com.example.investfeed.domain.assistant.service.TemplateFormat
import com.example.investfeed.domain.assistant.service.UsMarketCalendarService
import com.example.investfeed.domain.assistant.service.UsTreasuryFallbackService
import com.example.investfeed.domain.assistant.service.UsTreasuryService
import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.crawler.NaverMarketIndexCrawler
import com.example.investfeed.global.holiday.HolidayService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 비서 채팅 국채 금리 (get_treasury_yields). 한국 2~30년, 미국 2·5·10·30년.
 * - 날짜 미지정·오늘·직전 거래일: 네이버 (당일은 실시간, 직전 거래일은 일별 확정 종가)
 * - 그 전 날짜: 미국 FRED, 한국 ECOS 의 그날 값. 없으면 제공하지 않는다고 답한다
 */
@Service
class TreasuryToolService(
    private val marketFactSheetService: MarketFactSheetService,
    private val krTreasuryFallbackService: KrTreasuryFallbackService,
    private val usTreasuryFallbackService: UsTreasuryFallbackService,
    private val crawler: NaverMarketIndexCrawler,
    private val holidayService: HolidayService,
    private val usMarketCalendarService: UsMarketCalendarService,
) {
    enum class TreasuryMarket { KR, US }

    companion object {
        private val KR_TYPES = listOf(
            MarketIndexType.KR_TREASURY_2Y, MarketIndexType.KR_TREASURY_3Y, MarketIndexType.KR_TREASURY_5Y,
            MarketIndexType.KR_TREASURY_10Y, MarketIndexType.KR_TREASURY_20Y, MarketIndexType.KR_TREASURY_30Y,
        )
        private const val FRED_LOOKBACK_DAYS = 10L
        private const val NAVER_PAGE_SIZE = 20
    }

    fun treasuryYields(market: TreasuryMarket?, requested: AssistantDate?): Any {
        // 미국만 물었으면 미국 기준 날짜 (미국 "오늘" = 날짜 없이 직전 장), 아니면 한국 기준 날짜로 분기·제목을 정한다
        val date = requested?.resolve(if (market == TreasuryMarket.US) StockMarket.US else StockMarket.KR)
        val withKr = market != TreasuryMarket.US
        val withUs = market != TreasuryMarket.KR
        val now = LocalDateTime.now()
        val krToday = now.toLocalDate()
        val usLatest = usMarketCalendarService.lastClosedUsTradingDay()

        if (date == null) {
            val krDate = holidayService.lastTradingDay(krToday)
            val r = BriefingTemplateRenderer.renderTreasury(
                kr = if (withKr) marketFactSheetService.krTreasuryNow(krDate) else null,
                us = if (withUs) marketFactSheetService.usTreasuryNow(usLatest) else null,
                krNotice = null, usNotice = null, withKr = withKr, withUs = withUs,
            )
            return DataToolService.MarketCard(title(market, null), now, r.summary, r.sections)
        }

        val md = TemplateFormat.monthDay(date)
        var krTitleNote: String? = null
        val krNotice: String?
        val kr: KrTreasuryFact?
        if (!withKr) { krNotice = null; kr = null } else when {
            date.isAfter(krToday) -> { krNotice = "${md}은 아직 오지 않은 날짜입니다."; kr = null }
            holidayService.isHoliday(date) -> { krNotice = "${md}은 국내 채권시장 휴장일입니다."; kr = null }
            date == krToday -> { krNotice = null; kr = marketFactSheetService.krTreasuryNow(date) }
            else -> {
                // 직전 거래일은 네이버 일별 종가, 그 전은 ECOS. 출처마다 기준이 달라 같은 날도 1~2bp 다를 수 있어 ECOS 는 제목에 출처를 밝힌다
                val naver = date == holidayService.lastTradingDay(krToday.minusDays(1))
                kr = if (naver) krNaverOn(date) else krEcosOn(date)
                krNotice = if (kr == null) "${md} 한국 국채 금리는 제공되지 않습니다." else null
                krTitleNote = if (naver) "$md 종가" else "ECOS $md 종가"
            }
        }

        // 한국·미국 함께: 한국 오늘이면 한국은 오늘, 미국은 직전 장
        val usDate = requested.resolve(StockMarket.US) ?: usLatest
        val usMd = TemplateFormat.monthDay(usDate)
        val usNotice: String?
        val us: TreasuryFact?
        if (!withUs) { usNotice = null; us = null } else when {
            usDate.isAfter(usLatest) -> { usNotice = "미국 채권시장 현지 ${usMd} 장은 아직 끝나지 않았습니다."; us = null }
            !usMarketCalendarService.isUsTradingDay(usDate) || usMarketCalendarService.isUsBondHoliday(usDate) -> {
                usNotice = "미국 채권시장은 현지 ${usMd} 휴장일입니다."; us = null
            }
            // 직전 거래일(가장 최근에 끝난 장)은 네이버 (브리핑과 같은 조회), 그 전은 FRED
            usDate == usLatest -> { usNotice = null; us = marketFactSheetService.usTreasuryNow(usDate) }
            else -> {
                us = usFredOn(usDate)
                usNotice = if (us == null) "현지 ${usMd} 미국 국채 금리는 제공되지 않습니다." else null
            }
        }

        if (kr == null && us == null) return DataToolService.TextReply(listOfNotNull(krNotice, usNotice).joinToString("\n"))
        val r = BriefingTemplateRenderer.renderTreasury(kr, us, krNotice, usNotice, withKr, withUs, krTitleNote)
        return DataToolService.MarketCard(title(market, date), now, r.summary, r.sections)
    }

    private fun title(market: TreasuryMarket?, date: LocalDate?): String {
        val name = when (market) { TreasuryMarket.KR -> "한국 국채 금리"; TreasuryMarket.US -> "미국 국채 금리"; null -> "국채 금리" }
        return date?.let { "$name · ${TemplateFormat.monthDay(it)}" } ?: name
    }

    private fun krFact(f: (MarketIndexType) -> YieldFact?): KrTreasuryFact? =
        KR_TYPES.mapNotNull(f).takeIf { it.isNotEmpty() }?.let { KrTreasuryFact(it) }

    private fun krName(type: MarketIndexType) = type.displayName.removePrefix("한국 국채 ")

    /** 한국 직전 거래일: 네이버 일별 확정 종가 */
    private fun krNaverOn(date: LocalDate): KrTreasuryFact? =
        krFact { type -> naverOn(type, date)?.let { YieldFact(krName(type), it.close, it.changeBp) } }

    /** 한국 그 전 날짜: ECOS 그날 종가 */
    private fun krEcosOn(date: LocalDate): KrTreasuryFact? =
        krFact { type -> krTreasuryFallbackService.closeOn(type, date)?.let { YieldFact(krName(type), it.value, it.changeBp) } }

    /** 미국 그 전 날짜: FRED 그날 값. 변화는 직전 관측일 대비 */
    private fun usFredOn(date: LocalDate): TreasuryFact? {
        data class Y(val value: Double, val bp: Double?, val source: String)
        fun y(type: MarketIndexType): Y? {
            val series = UsTreasuryService.SERIES_OF[type] ?: return null
            val obs = usTreasuryFallbackService.observations(series, date.minusDays(FRED_LOOKBACK_DAYS), date)
            val i = obs.indexOfFirst { it.first == date }.takeIf { it >= 0 } ?: return null
            return Y(obs[i].second, obs.getOrNull(i - 1)?.let { (obs[i].second - it.second) * 100 }, UsTreasuryService.SOURCE_FRED)
        }
        val y2 = y(MarketIndexType.US_TREASURY_2Y)
        val y5 = y(MarketIndexType.US_TREASURY_5Y)
        val y10 = y(MarketIndexType.US_TREASURY_10Y)
        val y30 = y(MarketIndexType.US_TREASURY_30Y)
        val all = listOfNotNull(y2, y5, y10, y30)
        if (all.isEmpty()) return null
        return TreasuryFact(
            y2 = y2?.value, y2Bp = y2?.bp, y5 = y5?.value, y5Bp = y5?.bp,
            y10 = y10?.value, y10Bp = y10?.bp, y30 = y30?.value, y30Bp = y30?.bp,
            spreadBp = if (y2 != null && y10 != null) Math.round((y10.value - y2.value) * 100).toInt() else null,
            source = if (all.any { it.source == UsTreasuryService.SOURCE_FRED }) UsTreasuryService.SOURCE_FRED else UsTreasuryService.SOURCE_NAVER,
            asOfDate = date,
        )
    }

    private fun naverOn(type: MarketIndexType, date: LocalDate): NaverMarketIndexCrawler.BondDailyClose? =
        runCatching { crawler.fetchBondDailyCloses(type, NAVER_PAGE_SIZE) }.getOrDefault(emptyList()).firstOrNull { it.tradeDate == date }
}
