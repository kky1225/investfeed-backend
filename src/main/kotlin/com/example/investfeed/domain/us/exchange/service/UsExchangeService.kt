package com.example.investfeed.domain.us.exchange.service

import com.example.investfeed.domain.calendar.dto.res.IndicatorHistoryRes
import com.example.investfeed.domain.calendar.service.EconomicCalendarService
import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import com.example.investfeed.domain.us.exchange.dto.res.UsExchangeRateItem
import com.example.investfeed.domain.us.exchange.dto.res.UsExchangeRateRes
import com.example.investfeed.kiwoom.us.exchange.client.UsExchangeClient
import com.example.investfeed.kiwoom.us.exchange.dto.req.KiwoomUsExchangeRateReq
import com.example.investfeed.kiwoom.us.exchange.dto.res.KiwoomUsExchangeRateRes
import org.springframework.stereotype.Service

@Service
class UsExchangeService(
    private val usExchangeClient: UsExchangeClient,
    private val marketIndexService: MarketIndexService,
    private val economicCalendarService: EconomicCalendarService,
) {
    companion object {
        private const val EXCH_TP_KRW_TO_USD = "1"
        private const val EXCH_TP_USD_TO_KRW = "2"
        private const val USD_KRW_ECOS_CODE = "731Y003"
        private const val USD_KRW_ECOS_COUNTRY = "KR"
    }

    fun getExchangeRate(): UsExchangeRateRes {
        val krwToUsd = usExchangeClient.usExchangeRate(
            req = KiwoomUsExchangeRateReq(exch_tp = EXCH_TP_KRW_TO_USD)
        )
        val usdToKrw = usExchangeClient.usExchangeRate(
            req = KiwoomUsExchangeRateReq(exch_tp = EXCH_TP_USD_TO_KRW)
        )

        return UsExchangeRateRes(
            krwToUsd = krwToUsd.toItem(EXCH_TP_KRW_TO_USD),
            usdToKrw = usdToKrw.toItem(EXCH_TP_USD_TO_KRW),
            marketIndex = marketIndexService.getMarketIndex(MarketIndexType.USD_KRW)
        )
    }

    fun getUsdKrwHistory(): IndicatorHistoryRes? {
        return economicCalendarService.getIndicatorHistory(USD_KRW_ECOS_CODE, USD_KRW_ECOS_COUNTRY)
    }

    private fun KiwoomUsExchangeRateRes.toItem(exchTp: String) = UsExchangeRateItem(
        exchTp = exchTp,
        sellAplcExrt = sell_aplc_exrt,
        buyAplcExrt = buy_aplc_exrt,
        aplcExrt = aplc_exrt,
        exrtTpNm = exrt_tp_nm,
        spclBfExrt = spcl_bf_exrt,
        exrtSpclRt = exrt_spcl_rt,
    )
}
