package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.ecos.client.EcosClient
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 네이버 국고채 시세 결측 시 ECOS 817Y002(시장금리 일별, 금투협 최종호가수익률) 예비 조회.
 * ECOS 는 당일 값이 다음 날 반영되므로 장 마감 브리핑 시점엔 보통 전 거래일 종가가 나온다
 */
@Service
class KrTreasuryFallbackService(
    private val ecosClient: EcosClient,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val SOURCE_ECOS = "ECOS"
        private const val TABLE_CODE = "817Y002"
        private const val LOOKBACK_DAYS = 10L
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val ECOS_ITEMS = mapOf(
            MarketIndexType.KR_TREASURY_2Y to "010195000",
            MarketIndexType.KR_TREASURY_3Y to "010200000",
            MarketIndexType.KR_TREASURY_5Y to "010200001",
            MarketIndexType.KR_TREASURY_10Y to "010210000",
            MarketIndexType.KR_TREASURY_20Y to "010220000",
            MarketIndexType.KR_TREASURY_30Y to "010230000",
        )
    }

    data class Close(val tradeDate: LocalDate, val value: Double, val changeBp: Double?)

    /** 그날 종가만 (비서 날짜 지정 조회). 그날 행이 없으면(휴장·아직 미반영) null. 변화는 직전 행 대비 */
    fun closeOn(type: MarketIndexType, date: LocalDate): Close? {
        val obs = observations(type, date) ?: return null
        val i = obs.indexOfFirst { it.first == date }.takeIf { it >= 0 } ?: return null
        val value = obs[i].second
        return Close(date, value, obs.getOrNull(i - 1)?.let { (value - it.second) * 100 })
    }

    private fun observations(type: MarketIndexType, upTo: LocalDate): List<Pair<LocalDate, Double>>? {
        val itemCode = ECOS_ITEMS[type] ?: return null
        return try {
            ecosClient.getStatistics(
                TABLE_CODE, "D", upTo.minusDays(LOOKBACK_DAYS).format(YYYYMMDD), upTo.format(YYYYMMDD), itemCode, maxRows = 20,
            ).statisticSearch?.row.orEmpty()
                .mapNotNull { row ->
                    val date = row.TIME?.let { runCatching { LocalDate.parse(it, YYYYMMDD) }.getOrNull() } ?: return@mapNotNull null
                    val value = row.DATA_VALUE?.toDoubleOrNull() ?: return@mapNotNull null
                    date to value
                }
                .sortedBy { it.first }
        } catch (e: Exception) {
            log.error(e) { "ECOS $TABLE_CODE/$itemCode 조회 실패 (~$upTo)" }
            null
        }
    }

    fun latestClose(type: MarketIndexType, tradeDate: LocalDate): Close? {
        val itemCode = ECOS_ITEMS[type] ?: return null
        return try {
            val obs = ecosClient.getStatistics(
                TABLE_CODE, "D", tradeDate.minusDays(LOOKBACK_DAYS).format(YYYYMMDD), tradeDate.format(YYYYMMDD), itemCode, maxRows = 20,
            ).statisticSearch?.row.orEmpty()
                .mapNotNull { row ->
                    val date = row.TIME?.let { runCatching { LocalDate.parse(it, YYYYMMDD) }.getOrNull() } ?: return@mapNotNull null
                    val value = row.DATA_VALUE?.toDoubleOrNull() ?: return@mapNotNull null
                    date to value
                }
                .sortedBy { it.first }
            val (date, value) = obs.lastOrNull() ?: return null
            Close(date, value, obs.getOrNull(obs.size - 2)?.let { (value - it.second) * 100 })
        } catch (e: Exception) {
            log.error(e) { "ECOS $TABLE_CODE/$itemCode 조회 실패 (~$tradeDate)" }
            null
        }
    }
}
