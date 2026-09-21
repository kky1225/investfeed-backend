package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.*
import com.example.investfeed.domain.assistant.dto.message.Headline
import com.example.investfeed.domain.assistant.dto.message.HeadlineScope
import com.example.investfeed.domain.assistant.service.TemplateFormat.rate
import com.example.investfeed.domain.assistant.service.TemplateFormat.monthDay
import com.example.investfeed.domain.assistant.service.TemplateFormat.num
import kotlin.math.abs

object HeadlineSelector {
    private val HEADLINE_US_INDEXES = listOf("나스닥", "S&P500", "다우")

    fun selectUsClose(sheet: UsCloseFactSheet): Headline {
        val market = usCloseMarket(sheet)
        return Headline(market, HeadlineScope.MARKET, market)
    }

    private fun usCloseMarket(sheet: UsCloseFactSheet): String {
        val idx = sheet.indexes.orEmpty().associateBy { it.name }
        return HEADLINE_US_INDEXES.joinToString(" · ") { "$it ${rateOr(idx[it]?.changeRate)}" }
    }

    fun selectKrPre(sheet: KrPreFactSheet): Headline {
        val market = krPreMarket(sheet)
        return Headline(market, HeadlineScope.MARKET, market)
    }

    private fun krPreMarket(sheet: KrPreFactSheet): String {
        val prev = sheet.krPrev
        val day = prev?.tradeDate?.let { "${monthDay(it)} " } ?: ""
        val fx = sheet.macro?.usdKrw
        val indexes = "${day}코스피 ${rateOr(prev?.kospi?.changeRate, 2)} · 코스닥 ${rateOr(prev?.kosdaq?.changeRate, 2)}"
        val fxPart = fx?.changeRate?.takeIf { abs(it) >= 1.0 }?.let { " · 원/달러 ${num(fx.price, 0)} (${rate(it)})" } ?: ""
        return indexes + fxPart
    }

    fun selectCoinDaily(sheet: CoinDailyFactSheet): Headline {
        val market = coinMarket(sheet)
        return Headline(market, HeadlineScope.MARKET, market)
    }

    private fun coinMarket(sheet: CoinDailyFactSheet): String {
        val fg = sheet.fearGreed
        return listOfNotNull(
            sheet.btc?.changeRate?.let { "BTC ${rate(it)}" }, sheet.eth?.changeRate?.let { "ETH ${rate(it)}" },
            fg?.let { "공포탐욕 $fg (${TemplateFormat.fearGreedLabel(fg)})" },
        ).joinToString(" · ").ifEmpty { "코인 시세 없음" }
    }

    fun selectKrClose(sheet: KrCloseFactSheet): Headline {
        val market = krCloseMarket(sheet)
        return Headline(market, HeadlineScope.MARKET, market)
    }

    fun selectKrHoldings(): Headline {
        val text = "국내 계좌 마감 · 애프터마켓 20:00 기준"
        return Headline(text, HeadlineScope.MARKET, text)
    }

    private fun krCloseMarket(sheet: KrCloseFactSheet): String =
        "코스피 ${rateOr(sheet.kospi?.changeRate, 2)} · 코스닥 ${rateOr(sheet.kosdaq?.changeRate, 2)}"

    private fun rateOr(v: Double?, digits: Int = 1) = v?.let { rate(it, digits) } ?: "-"
}
