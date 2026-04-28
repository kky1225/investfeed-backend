package com.example.investfeed.domain.crypto.service

import com.example.investfeed.domain.crypto.dto.req.CryptoChartType
import com.example.investfeed.domain.crypto.dto.req.CryptoDetailReq
import com.example.investfeed.domain.crypto.dto.res.*
import com.example.investfeed.feargreed.client.FearGreedClient
import com.example.investfeed.upbit.candle.client.CandleClient
import com.example.investfeed.upbit.config.CryptoType
import com.example.investfeed.upbit.market.client.MarketClient
import com.example.investfeed.upbit.ticker.client.TickerClient
import com.example.investfeed.upbit.websocket.client.CryptoStreamClient
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class CryptoService(
    private val tickerClient: TickerClient,
    private val candleClient: CandleClient,
    private val fearGreedClient: FearGreedClient,
    private val cryptoStreamClient: CryptoStreamClient,
    private val marketClient: MarketClient,
) {
    fun streamCryptos() {
        val markets = CryptoType.entries.map { it.market }
        cryptoStreamClient.cryptoListStream(markets)
    }

    fun streamCrypto(market: String) {
        cryptoStreamClient.cryptoListStream(listOf(market))
    }

    fun listCryptos(): CryptoListRes {
        val cryptoTypes = CryptoType.entries
        val markets = cryptoTypes.joinToString(",") { it.market }

        val tickers = tickerClient.getTickers(markets)

        val cryptoList = cryptoTypes.map { cryptoType ->
            val ticker = tickers.find { it.market == cryptoType.market }

            // 3분봉 480개 = 24시간 (API 최대 200개씩 페이징)
            val allCandles = mutableListOf<CryptoChartMinute>()
            var to: String? = null
            val totalNeeded = 480

            while (allCandles.size < totalNeeded) {
                val remaining = totalNeeded - allCandles.size
                val fetchCount = minOf(remaining, 200)

                val candles = candleClient.getCandlesMinutes(
                    unit = 3,
                    market = cryptoType.market,
                    count = fetchCount,
                    to = to,
                )

                if (candles.isEmpty()) break

                allCandles.addAll(candles.map {
                    CryptoChartMinute(
                        tradePrice = it.trade_price,
                        candleDateTimeKst = it.candle_date_time_kst,
                    )
                })

                // Upbit API의 to 파라미터는 UTC 시간을 기대하므로 candle_date_time_utc 사용
                to = candles.last().candle_date_time_utc
            }

            val chartMinuteList = allCandles.reversed()

            CryptoListItem(
                market = cryptoType.market,
                koreanName = cryptoType.koreanName,
                englishName = cryptoType.englishName,
                tradePrice = ticker?.trade_price,
                change = ticker?.change,
                changeRate = ticker?.change_rate,
                changePrice = ticker?.change_price,
                signedChangeRate = ticker?.signed_change_rate,
                signedChangePrice = ticker?.signed_change_price,
                accTradePrice24h = ticker?.acc_trade_price_24h,
                accTradeVolume24h = ticker?.acc_trade_volume_24h,
                highest52WeekPrice = ticker?.highest_52_week_price,
                highest52WeekDate = ticker?.highest_52_week_date,
                lowest52WeekPrice = ticker?.lowest_52_week_price,
                lowest52WeekDate = ticker?.lowest_52_week_date,
                tradeDateTimeKst = "${ticker?.trade_date_kst} ${ticker?.trade_time_kst}",
                chartMinuteList = chartMinuteList,
            )
        }

        val fearGreed = fearGreedIndex()

        return CryptoListRes(cryptoList = cryptoList, fearGreed = fearGreed)
    }

    fun getCrypto(market: String, req: CryptoDetailReq): CryptoDetailRes {
        val cryptoType = CryptoType.entries.find { it.market == market }
        val koreanName: String
        val englishName: String

        if (cryptoType != null) {
            koreanName = cryptoType.koreanName
            englishName = cryptoType.englishName
        } else {
            val marketInfo = marketClient.getKrwMarkets().find { it.market == market }
                ?: throw IllegalArgumentException("Invalid market: ${market}")
            koreanName = marketInfo.korean_name
            englishName = marketInfo.english_name
        }

        val ticker = tickerClient.getTickers(market).firstOrNull()

        val chartList: List<CryptoChart> = when (req.chartType) {
            CryptoChartType.MINUTE_1,
            CryptoChartType.MINUTE_3,
            CryptoChartType.MINUTE_5,
            CryptoChartType.MINUTE_10,
            CryptoChartType.MINUTE_30 -> {
                val unit = req.chartType.unit!!
                val totalNeeded = when (req.chartType) {
                    CryptoChartType.MINUTE_1 -> 200   // 약 3.3시간
                    CryptoChartType.MINUTE_3 -> 480   // 24시간
                    CryptoChartType.MINUTE_5 -> 288   // 24시간
                    CryptoChartType.MINUTE_10 -> 144  // 24시간
                    CryptoChartType.MINUTE_30 -> 48   // 24시간
                    else -> 200
                }
                fetchMinuteCandles(unit, market, totalNeeded)
            }

            CryptoChartType.DAY -> {
                val allCandles = mutableListOf<com.example.investfeed.upbit.candle.dto.res.UpbitCandleDayRes>()
                var to: String? = null
                repeat(5) {
                    val batch = candleClient.getCandlesDays(market = market, count = 200, to = to)
                    if (batch.isEmpty()) return@repeat
                    allCandles.addAll(batch)
                    to = batch.last().candle_date_time_utc
                }
                allCandles.reversed().map {
                    CryptoChart(
                        dt = it.candle_date_time_kst,
                        tradePrice = it.trade_price,
                        openingPrice = it.opening_price,
                        highPrice = it.high_price,
                        lowPrice = it.low_price,
                        candleAccTradeVolume = it.candle_acc_trade_volume,
                        candleAccTradePrice = it.candle_acc_trade_price,
                    )
                }
            }

            CryptoChartType.WEEK -> {
                val allCandles = mutableListOf<com.example.investfeed.upbit.candle.dto.res.UpbitCandleWeekMonthRes>()
                var to: String? = null
                repeat(3) {
                    val batch = candleClient.getCandlesWeeks(market = market, count = 200, to = to)
                    if (batch.isEmpty()) return@repeat
                    allCandles.addAll(batch)
                    to = batch.last().candle_date_time_utc
                }
                allCandles.reversed().map {
                    CryptoChart(
                        dt = it.candle_date_time_kst,
                        tradePrice = it.trade_price,
                        openingPrice = it.opening_price,
                        highPrice = it.high_price,
                        lowPrice = it.low_price,
                        candleAccTradeVolume = it.candle_acc_trade_volume,
                        candleAccTradePrice = it.candle_acc_trade_price,
                    )
                }
            }

            CryptoChartType.MONTH -> {
                val first = candleClient.getCandlesMonths(market = market, count = 200)
                val oldestTime = first.lastOrNull()?.candle_date_time_utc
                val second = if (oldestTime != null) candleClient.getCandlesMonths(market = market, count = 200, to = oldestTime) else emptyList()
                (second + first).reversed().map {
                    CryptoChart(
                        dt = it.candle_date_time_kst,
                        tradePrice = it.trade_price,
                        openingPrice = it.opening_price,
                        highPrice = it.high_price,
                        lowPrice = it.low_price,
                        candleAccTradeVolume = it.candle_acc_trade_volume,
                        candleAccTradePrice = it.candle_acc_trade_price,
                    )
                }
            }

            CryptoChartType.YEAR -> {
                candleClient.getCandlesYears(market = market, count = 200).reversed().map {
                    CryptoChart(
                        dt = it.candle_date_time_kst,
                        tradePrice = it.trade_price,
                        openingPrice = it.opening_price,
                        highPrice = it.high_price,
                        lowPrice = it.low_price,
                        candleAccTradeVolume = it.candle_acc_trade_volume,
                        candleAccTradePrice = it.candle_acc_trade_price,
                    )
                }
            }
        }

        val cryptoInfo = CryptoDetailInfo(
            market = market,
            koreanName = koreanName,
            englishName = englishName,
            tradePrice = ticker?.trade_price,
            openingPrice = ticker?.opening_price,
            highPrice = ticker?.high_price,
            lowPrice = ticker?.low_price,
            prevClosingPrice = ticker?.prev_closing_price,
            change = ticker?.change,
            signedChangeRate = ticker?.signed_change_rate,
            signedChangePrice = ticker?.signed_change_price,
            accTradePrice24h = ticker?.acc_trade_price_24h,
            accTradeVolume24h = ticker?.acc_trade_volume_24h,
            highest52WeekPrice = ticker?.highest_52_week_price,
            highest52WeekDate = ticker?.highest_52_week_date,
            lowest52WeekPrice = ticker?.lowest_52_week_price,
            lowest52WeekDate = ticker?.lowest_52_week_date,
            tradeDateTimeKst = "${ticker?.trade_date_kst} ${ticker?.trade_time_kst}",
        )

        return CryptoDetailRes(
            cryptoInfo = cryptoInfo,
            chartList = chartList,
        )
    }

    fun fearGreedIndex(): FearGreedRes {
        val apiRes = fearGreedClient.getFearGreedIndex(limit = 30)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("Asia/Seoul"))

        val items = apiRes.data?.map { data ->
            FearGreedItem(
                value = data.value?.toIntOrNull() ?: 0,
                classification = data.value_classification ?: "",
                date = data.timestamp?.toLongOrNull()?.let {
                    dateFormatter.format(Instant.ofEpochSecond(it))
                } ?: "",
            )
        } ?: emptyList()

        return FearGreedRes(
            current = items.firstOrNull() ?: FearGreedItem(),
            history = items,
        )
    }

    fun searchCryptos(keyword: String): List<CryptoSearchItem> {
        val lower = keyword.lowercase()
        return marketClient.getKrwMarkets()
            .filter {
                it.korean_name.contains(keyword) ||
                        it.english_name.lowercase().contains(lower) ||
                        it.market.lowercase().contains(lower)
            }
            .take(20)
            .map {
                CryptoSearchItem(
                    market = it.market,
                    koreanName = it.korean_name,
                    englishName = it.english_name,
                )
            }
    }

    private fun fetchMinuteCandles(unit: Int, market: String, totalNeeded: Int): List<CryptoChart> {
        val allCandles = mutableListOf<CryptoChart>()
        var to: String? = null

        while (allCandles.size < totalNeeded) {
            val remaining = totalNeeded - allCandles.size
            val fetchCount = minOf(remaining, 200)

            val candles = candleClient.getCandlesMinutes(
                unit = unit,
                market = market,
                count = fetchCount,
                to = to,
            )

            if (candles.isEmpty()) break

            allCandles.addAll(candles.map {
                CryptoChart(
                    dt = it.candle_date_time_kst,
                    tradePrice = it.trade_price,
                    openingPrice = it.opening_price,
                    highPrice = it.high_price,
                    lowPrice = it.low_price,
                    candleAccTradeVolume = it.candle_acc_trade_volume,
                    candleAccTradePrice = it.candle_acc_trade_price,
                )
            })

            to = candles.last().candle_date_time_utc
        }

        return allCandles.reversed()
    }

    fun listCryptoRanks(): List<CryptoRankItem> {
        val krwMarkets = marketClient.getKrwMarkets()
        val marketCodes = krwMarkets.joinToString(",") { it.market }

        val tickers = tickerClient.getTickers(marketCodes)

        return tickers.mapNotNull { ticker ->
            val marketInfo = krwMarkets.find { it.market == ticker.market } ?: return@mapNotNull null

            CryptoRankItem(
                market = ticker.market ?: "",
                koreanName = marketInfo.korean_name,
                englishName = marketInfo.english_name,
                tradePrice = ticker.trade_price ?: 0.0,
                signedChangePrice = ticker.signed_change_price ?: 0.0,
                signedChangeRate = ticker.signed_change_rate ?: 0.0,
                change = ticker.change ?: "EVEN",
                accTradePrice24h = ticker.acc_trade_price_24h ?: 0.0,
                accTradeVolume24h = ticker.acc_trade_volume_24h ?: 0.0,
                highPrice = ticker.high_price ?: 0.0,
                lowPrice = ticker.low_price ?: 0.0,
                prevClosingPrice = ticker.prev_closing_price ?: 0.0,
                warning = marketInfo.market_event?.warning ?: false,
            )
        }.sortedByDescending { it.accTradePrice24h }
    }

    fun streamCryptoRanks() {
        val krwMarkets = marketClient.getKrwMarkets()
        val markets = krwMarkets.map { it.market }
        cryptoStreamClient.cryptoListStream(markets)
    }
}
