package com.example.investfeed.domain.dividend.service

import com.example.investfeed.domain.dividend.client.EtfDividendClient
import com.example.investfeed.domain.dividend.client.StockDividendClient
import com.example.investfeed.domain.dividend.client.dto.DividendApiItem
import com.example.investfeed.domain.dividend.dto.res.StockDividendRes
import com.example.investfeed.domain.dividend.entity.StockDividend
import com.example.investfeed.domain.dividend.repository.StockDividendRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class StockDividendService(
    private val stockDividendClient: StockDividendClient,
    private val etfDividendClient: EtfDividendClient,
    private val stockDividendRepository: StockDividendRepository,
) {
    private val log = KotlinLogging.logger {}

    private val NUM_OF_ROWS = 5000
    private val YEARS_TO_KEEP = 6
    private val DISPLAY_YEAR_COUNT = 5
    private val TYPE_STOCK = "STOCK"
    private val TYPE_ETF = "ETF"
    private val MARKET_CODE_ETF = "8"

    fun collectDailyDividends() {
        val today = today()
        val minDvdnBasDt = minDvdnBasDt()
        val existingKeys = stockDividendRepository
            .findExistingKeysByTypeAndMinDvdnBasDt(TYPE_STOCK, minDvdnBasDt)
            .toMutableSet()

        var pageNo = 1
        var totalPages = 1
        var saved = 0

        while (pageNo <= totalPages) {
            val (items, totalCount) = stockDividendClient.getDividendInfo(pageNo = pageNo, numOfRows = NUM_OF_ROWS)
            if (pageNo == 1) {
                totalPages = (totalCount + NUM_OF_ROWS - 1) / NUM_OF_ROWS
            }
            if (items.isEmpty()) break

            val recentItems = items.filter { (it.dvdnBasDt ?: "") >= minDvdnBasDt }
            saved += saveStockItems(recentItems, today, existingKeys)
            pageNo++
        }

        log.info { "배당 정보 일별 수집: ${totalPages}페이지 조회, $saved 건 저장" }
    }

    fun collectEtfDividends(stkCd: String): Int {
        val today = today()
        val minDvdnBasDt = minDvdnBasDt()
        val existingDates = stockDividendRepository
            .findDvdnBasDtsByStkCdAndType(stkCd, TYPE_ETF)
            .toMutableSet()

        val items = etfDividendClient.getDividendInfo(stkCd)
        var count = 0

        items.forEach { item ->
            try {
                val dvdnBasDt = item.date?.replace("-", "") ?: return@forEach
                if (dvdnBasDt < minDvdnBasDt) return@forEach
                if (!existingDates.add(dvdnBasDt)) return@forEach // 이미 존재 → skip

                stockDividendRepository.save(
                    StockDividend(
                        stkCd = stkCd,
                        type = TYPE_ETF,
                        basDt = today,
                        createdDt = today,
                        dvdnBasDt = dvdnBasDt,
                        stckGenrDvdnAmt = item.amount?.toString(),
                        stckDvdnRcdNm = "분배금",
                    )
                )
                count++
            } catch (e: Exception) {
                log.warn(e) { "ETF 분배금 저장 실패: stkCd=$stkCd, date=${item.date}" }
            }
        }
        return count
    }

    fun getDividendList(stkCd: String, marketCode: String? = null): List<StockDividendRes> {
        val cleanStkCd = stkCd.substringBefore("_")

        // marketCode가 8이면 ETF → createdDt 기준 1일 1회 수집
        if (marketCode == MARKET_CODE_ETF) {
            val today = today()
            val latest = stockDividendRepository.findFirstByStkCdAndTypeOrderByCreatedDtDesc(cleanStkCd, TYPE_ETF)
            if (latest == null || latest.createdDt != today) {
                collectEtfDividends(cleanStkCd)
            }
        }

        val minDvdnBasDt = minDvdnBasDt()
        val dividends = stockDividendRepository.findByStkCdAndDvdnBasDtGreaterThanEqualAndStckDvdnRcdNmNotOrderByDvdnBasDtDesc(
            stkCd = cleanStkCd,
            dvdnBasDt = minDvdnBasDt,
            stckDvdnRcdNm = "무배당",
        )

        val recentYears = dividends
            .mapNotNull { it.dvdnBasDt?.substring(0, 4) }
            .distinct()
            .sortedDescending()
            .take(DISPLAY_YEAR_COUNT)
            .toSet()

        return dividends
            .filter { it.dvdnBasDt?.substring(0, 4) in recentYears }
            .map { it.toRes() }
    }

    private fun saveStockItems(
        items: List<DividendApiItem>,
        today: String,
        existingKeys: MutableSet<String>,
    ): Int {
        var count = 0
        items.forEach { item ->
            try {
                val isinCd = item.isinCd
                if (isinCd.isNullOrBlank() || isinCd.length < 9) return@forEach

                val stkCd = isinCd.substring(3, 9)
                val key = "$stkCd|${item.dvdnBasDt ?: ""}|${item.scrsItmsKcd ?: ""}"
                if (!existingKeys.add(key)) return@forEach // 이미 존재 → skip

                stockDividendRepository.save(
                    StockDividend(
                        stkCd = stkCd,
                        type = TYPE_STOCK,
                        basDt = item.basDt ?: "",
                        createdDt = today,
                        crno = item.crno,
                        isinCd = item.isinCd,
                        stckIssuCmpyNm = item.stckIssuCmpyNm,
                        isinCdNm = item.isinCdNm,
                        scrsItmsKcd = item.scrsItmsKcd,
                        scrsItmsKcdNm = item.scrsItmsKcdNm,
                        stckParPrc = item.stckParPrc,
                        stckStacMd = item.stckStacMd,
                        dvdnBasDt = item.dvdnBasDt,
                        cashDvdnPayDt = item.cashDvdnPayDt,
                        stckHndvDt = item.stckHndvDt,
                        stckDvdnRcd = item.stckDvdnRcd,
                        stckDvdnRcdNm = item.stckDvdnRcdNm,
                        stckGenrDvdnAmt = item.stckGenrDvdnAmt,
                        stckGrdnDvdnAmt = item.stckGrdnDvdnAmt,
                        stckGenrCashDvdnRt = item.stckGenrCashDvdnRt,
                        stckGenrDvdnRt = item.stckGenrDvdnRt,
                        cashGrdnDvdnRt = item.cashGrdnDvdnRt,
                        stckGrdnDvdnRt = item.stckGrdnDvdnRt,
                    )
                )
                count++
            } catch (e: Exception) {
                log.warn(e) { "배당 정보 저장 실패: isinCd=${item.isinCd}" }
            }
        }
        return count
    }

    private fun today() = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    private fun minDvdnBasDt() = LocalDate.now().minusYears(YEARS_TO_KEEP.toLong())
        .format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    private fun StockDividend.toRes() = StockDividendRes(
        dvdnBasDt = dvdnBasDt,
        stckDvdnRcdNm = stckDvdnRcdNm,
        stckGenrDvdnAmt = stckGenrDvdnAmt,
        cashDvdnPayDt = cashDvdnPayDt,
    )
}
