package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.*
import com.example.investfeed.domain.assistant.dto.message.Headline
import com.example.investfeed.domain.assistant.dto.message.HeadlineScope
import com.example.investfeed.domain.assistant.service.TemplateFormat.rate
import com.example.investfeed.domain.assistant.service.TemplateFormat.num
import com.example.investfeed.domain.assistant.service.TemplateFormat.signedUsd
import com.example.investfeed.domain.assistant.service.TemplateFormat.signedWon
import kotlin.math.abs

object HeadlineSelector {
    fun selectUsClose(sheet: UsCloseFactSheet, personal: PersonalFactSheet?): Headline {
        val market = usCloseMarket(sheet)
        val p = personal ?: return Headline(market.first, HeadlineScope.MARKET, market.first)
        val assetRate = p.usAssetDayRate() ?: return Headline(market.first, HeadlineScope.MARKET, market.first)
        val dayUsd = p.usDayChangeUsd() ?: 0.0
        val items = p.allItems(p.usBrokers).filter { it.dayRate != null }
        val top = items.maxByOrNull { abs(it.dayRate!!) }
        val big = items.filter { abs(it.dayRate!!) >= 5.0 }.maxByOrNull { abs(it.dayRate!!) }
        val text = when {
            big != null -> "내 미국 자산 ${rate(assetRate)} (${signedUsd(dayUsd)}). ${big.name} ${rate(big.dayRate!!)}"
            !market.second && top != null -> "내 미국 자산 ${rate(assetRate)} (${signedUsd(dayUsd)}). ${top.name} ${rate(top.dayRate!!)}"
            else -> return Headline(market.first, HeadlineScope.MARKET, market.first)
        }
        return Headline(text, HeadlineScope.PERSONAL, market.first)
    }

    private fun usCloseMarket(sheet: UsCloseFactSheet): Pair<String, Boolean> {
        val idx = sheet.indexes.orEmpty().associateBy { it.name }
        val nasdaq = idx["나스닥"]?.changeRate
        val spx = idx["S&P500"]?.changeRate
        val vix = idx["VIX"]?.close
        if (vix != null && vix >= 30) return "VIX ${num(vix, 1)} 변동성 고조. 나스닥 ${rateOr(nasdaq)}" to true
        val mover = listOf("나스닥" to nasdaq, "S&P500" to spx).filter { it.second != null && abs(it.second!!) >= 2.0 }.maxByOrNull { abs(it.second!!) }
        if (mover != null) return "미국장 ${mover.first} ${rate(mover.second!!)}. VIX ${vix?.let { num(it, 1) } ?: "-"}" to true
        return "나스닥 ${rateOr(nasdaq)} · S&P500 ${rateOr(spx)} · VIX ${vix?.let { num(it, 1) } ?: "-"}" to false
    }

    fun selectKrPre(sheet: KrPreFactSheet): Headline {
        val market = krPreMarket(sheet)
        return Headline(market, HeadlineScope.MARKET, market)
    }

    private fun krPreMarket(sheet: KrPreFactSheet): String {
        val kr = sheet.krPrev?.kospi?.changeRate
        val fx = sheet.macro?.usdKrw
        if (fx?.changeRate != null && abs(fx.changeRate) >= 1.0) return "원/달러 ${num(fx.price, 0)} (${rate(fx.changeRate)})"
        if (kr != null && abs(kr) >= 2.0) return "어제 코스피 ${rate(kr, 2)}"
        return "어제 코스피 ${rateOr(kr, 2)} · 원/달러 ${fx?.let { num(it.price, 0) } ?: "-"}"
    }

    fun selectCoinDaily(sheet: CoinDailyFactSheet, personal: PersonalFactSheet?): Headline {
        val market = coinMarket(sheet)
        val p = personal ?: return Headline(market.first, HeadlineScope.MARKET, market.first)
        val ok = p.coinExchanges.orEmpty().filter { !it.failed }
        val eval = ok.sumOf { it.eval }
        val change = ok.mapNotNull { it.dayChange }.takeIf { it.isNotEmpty() }?.sum()
        val rateAll = change?.let { c -> (eval - c).takeIf { it > 0 }?.let { c / it * 100 } }
        val items = p.allItems(ok).filter { it.dayRate != null }
        val top = items.maxByOrNull { abs(it.dayRate!!) }
        if (rateAll == null || top == null) return Headline(market.first, HeadlineScope.MARKET, market.first)
        val big = items.filter { abs(it.dayRate!!) >= 5.0 }.maxByOrNull { abs(it.dayRate!!) }
        val text = when {
            abs(rateAll) >= 3.0 || big != null -> "내 코인 ${rate(rateAll)} (${signedWon(Math.round(change!!))}). ${(big ?: top).name} ${rate((big ?: top).dayRate!!)}"
            !market.second -> "내 코인 ${rate(rateAll)} (${signedWon(Math.round(change!!))}). ${top.name} ${rate(top.dayRate!!)}"
            else -> return Headline(market.first, HeadlineScope.MARKET, market.first)
        }
        return Headline(text, HeadlineScope.PERSONAL, market.first)
    }

    private fun coinMarket(sheet: CoinDailyFactSheet): Pair<String, Boolean> {
        val btc = sheet.btc?.changeRate
        val fg = sheet.fearGreed
        if (fg != null && (fg <= 24 || fg >= 76)) return "공포탐욕 $fg ${TemplateFormat.fearGreedLabel(fg)}. BTC ${rateOr(btc)}" to true
        if (btc != null && abs(btc) >= 3.0) return "BTC ${rate(btc)} (${num(sheet.btc.close, 0)})" to true
        return listOfNotNull(
            btc?.let { "BTC ${rate(it)}" }, sheet.eth?.changeRate?.let { "ETH ${rate(it)}" }, fg?.let { "공포탐욕 $fg (${TemplateFormat.fearGreedLabel(fg)})" },
        ).joinToString(" · ").ifEmpty { "코인 시세 없음" } to false
    }

    fun selectKrClose(sheet: KrCloseFactSheet): Headline {
        val market = krCloseMarket(sheet)
        return Headline(market.first, HeadlineScope.MARKET, market.first)
    }

    fun selectKrHoldings(personal: PersonalFactSheet?): Headline {
        val public = "국내 계좌 마감 · 애프터마켓 20:00 기준"
        val p = personal ?: return Headline(public, HeadlineScope.MARKET, public)
        val assetRate = p.krAssetDayRate() ?: return Headline(public, HeadlineScope.MARKET, public)
        val dayWon = p.krDayChangeWon() ?: 0L
        val items = p.allItems(p.krBrokers).filter { it.dayRate != null }
        val contributor = items.filter { it.dayChange != null }.maxByOrNull { abs(it.dayChange!!) }
        val top = items.maxByOrNull { abs(it.dayRate!!) }
        val big = items.filter { abs(it.dayRate!!) >= 5.0 }.maxByOrNull { abs(it.dayRate!!) }
        val text = when {
            abs(assetRate) >= 3.0 -> "내 국내 자산 ${rate(assetRate)} (${signedWon(dayWon)}). ${(contributor ?: top)?.let { "${it.name} ${rate(it.dayRate!!)}" } ?: ""}".trim()
            big != null -> "${big.name} ${rate(big.dayRate!!)}. 내 국내 자산 ${rate(assetRate)}"
            top != null -> "내 국내 자산 ${rate(assetRate)} (${signedWon(dayWon)}). ${top.name} ${rate(top.dayRate!!)}"
            else -> "내 국내 자산 ${rate(assetRate)} (${signedWon(dayWon)})"
        }
        return Headline(text, HeadlineScope.PERSONAL, public)
    }

    private fun krCloseMarket(sheet: KrCloseFactSheet): Pair<String, Boolean> {
        val kospi = sheet.kospi
        val kr = kospi?.changeRate
        val flow = sheet.flow?.kospi
        val foreign = flow?.foreign
        val foreignText = foreign?.let { "외국인 ${TemplateFormat.eok(it)} ${if (it >= 0) "순매수" else "순매도"}" } ?: "외국인 -"
        if (kr != null && abs(kr) >= 2.0) return "코스피 ${rate(kr, 2)}. $foreignText" to true
        if (kospi != null && kr != null && kospi.high != null && kospi.low != null && kospi.changeAmount != null) {
            val prevClose = kospi.close - kospi.changeAmount
            if (prevClose > 0) {
                val lowRate = (kospi.low / prevClose - 1) * 100
                val highRate = (kospi.high / prevClose - 1) * 100
                val extreme = if (abs(lowRate - kr) >= abs(highRate - kr)) lowRate else highRate
                if (abs(extreme - kr) >= 1.5) return "코스피 ${rate(kr, 2)}. 장중 ${rate(extreme, 2)}까지" to true
            }
        }
        val sector = sheet.sectors.orEmpty().filter { abs(it.changeRate) >= 3.0 }.maxByOrNull { abs(it.changeRate) }
        if (sector != null && kr != null) return "코스피 ${rate(kr, 2)}. ${sector.name} ${rate(sector.changeRate)}" to true
        if (flow != null && flow.foreignStreak >= 5 && foreign != null) return "외국인 ${flow.foreignStreak}일 연속 ${if (foreign >= 0) "순매수" else "순매도"}" to true
        return "코스피 ${rateOr(kr, 2)}. ${foreign?.let { "외국인 ${TemplateFormat.eok(it)}" } ?: "외국인 -"}" to false
    }

    private fun rateOr(v: Double?, digits: Int = 1) = v?.let { rate(it, digits) } ?: "-"
}
