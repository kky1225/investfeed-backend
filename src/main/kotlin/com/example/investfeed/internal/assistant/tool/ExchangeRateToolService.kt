package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.assistant.dto.factsheet.QuoteFact
import com.example.investfeed.domain.assistant.service.BriefingTemplateRenderer
import com.example.investfeed.domain.assistant.service.MarketFactSheetService
import com.example.investfeed.domain.assistant.service.TemplateFormat
import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.crawler.NaverMarketIndexCrawler
import com.example.investfeed.global.holiday.HolidayService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ExchangeRateToolService(
    private val marketFactSheetService: MarketFactSheetService,
    private val crawler: NaverMarketIndexCrawler,
    private val holidayService: HolidayService,
) {
    companion object {
        private const val MAX_LOOKBACK_YEARS = 1L
    }

    fun exchangeRates(requested: AssistantDate?): Any {
        val date = requested?.resolve(StockMarket.KR)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()

        if (date == null || date == today) {
            val usdKrw = marketFactSheetService.fetchQuote(MarketIndexType.USD_KRW)
            val dollarIndex = marketFactSheetService.fetchQuote(MarketIndexType.DOLLAR_INDEX)
            if (usdKrw == null && dollarIndex == null) throw ToolException("환율 조회에 실패했습니다")
            val r = BriefingTemplateRenderer.renderExchangeRates(usdKrw, dollarIndex)
            return DataToolService.MarketCard("환율", now, r.summary, r.sections)
        }

        val md = TemplateFormat.monthDay(date)
        when {
            date.isAfter(today) -> return DataToolService.TextReply("${md}은 아직 오지 않은 날짜입니다.")
            date.isBefore(today.minusYears(MAX_LOOKBACK_YEARS)) -> return DataToolService.TextReply("환율은 최근 1년까지만 조회할 수 있습니다.")
        }
        // 은행 고시일과 증시 휴장일이 다를 수 있어(연말 등) 휴일 판정 전에 먼저 조회한다
        val close = crawler.fetchExchangeCloseOn(MarketIndexType.USD_KRW, date)
            ?: return DataToolService.TextReply(if (holidayService.isHoliday(date)) "${md}은 휴일이라 고시 환율이 없습니다." else "$md 원/달러 환율은 제공되지 않습니다.")
        val r = BriefingTemplateRenderer.renderExchangeRates(QuoteFact(price = close.close, changeRate = close.changeRate, changeAmount = close.change), null)
        return DataToolService.MarketCard("환율 · $md 최종 고시", now, r.summary, r.sections)
    }
}
