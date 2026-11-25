package com.example.investfeed.domain.dividend.service

import com.example.investfeed.domain.dividend.client.EtfDividendClient
import com.example.investfeed.domain.dividend.client.StockDividendClient
import com.example.investfeed.domain.dividend.client.dto.DividendApiItem
import com.example.investfeed.domain.dividend.dto.res.StockDividendRes
import com.example.investfeed.domain.dividend.entity.StockDividend
import com.example.investfeed.domain.dividend.repository.StockDividendRepository
import com.example.investfeed.common.util.failure
import com.example.investfeed.global.constant.RedisKeyPrefix
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

@Service
class StockDividendService(
    private val stockDividendClient: StockDividendClient,
    private val etfDividendClient: EtfDividendClient,
    private val stockDividendRepository: StockDividendRepository,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    private val NUM_OF_ROWS = 5000
    private val YEARS_TO_KEEP = 6
    private val DISPLAY_YEAR_COUNT = 5
    private val TYPE_STOCK = "STOCK"
    private val TYPE_ETF = "ETF"
    private val MARKET_CODE_ETF = "8"

    private val FETCH_RUNNING = "RUNNING"
    private val FETCH_DONE = "DONE"
    private val FETCH_FAILED = "FAILED"
    private val FETCH_RUNNING_TTL: Duration = Duration.ofMinutes(1)   // 외부 응답 대기(최대 20초)보다 길게
    private val FETCH_FAILED_TTL: Duration = Duration.ofMinutes(10)

    /** 결과 요약을 돌려준다 (성공 로그는 SchedulerLogService.execute 가 남김) */
    fun collectDailyDividends(): String {
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

        return "${totalPages}페이지 조회, ${saved}건 저장"
    }

    /** 외부에서 받은 ETF 분배금 중 새 것만 저장하고, 저장에 실패한 건수를 돌려준다 */
    private suspend fun collectEtfDividends(stkCd: String): Int {
        val today = today()
        val minDvdnBasDt = minDvdnBasDt()
        val existingDates = stockDividendRepository
            .findDvdnBasDtsByStkCdAndType(stkCd, TYPE_ETF)
            .toMutableSet()

        val items = etfDividendClient.getDividendInfo(stkCd)
        var failed = 0

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
            } catch (e: Exception) {
                failed++
                log.failure(e) { "ETF 분배금 저장 실패: stkCd=$stkCd, date=${item.date}" }
            }
        }
        return failed
    }

    suspend fun getDividendList(stkCd: String, marketCode: String? = null): List<StockDividendRes> {
        val cleanStkCd = stkCd.substringBefore("_")

        // marketCode가 8이면 ETF → 볼 때 외부에서 받아 온다 (성공은 하루 1회, 실패는 10분 뒤 재시도)
        if (marketCode == MARKET_CODE_ETF) {
            refreshEtfDividendsIfDue(cleanStkCd)
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
                log.failure(e) { "배당 정보 저장 실패: isinCd=${item.isinCd}" }
            }
        }
        return count
    }

    /**
     * ETF 분배금을 외부에서 받아 저장한다. 새 분배금이 없어도 성공하면 그날은 다시 부르지 않고,
     * 실패하면 10분 동안 부르지 않는다. 실패해도 화면은 막지 않고 이미 저장된 분배금으로 응답한다.
     * 키를 먼저 잡아 같은 종목 요청이 동시에 들어와도 외부 호출은 한 번만 나간다
     */
    private suspend fun refreshEtfDividendsIfDue(stkCd: String) {
        val key = "${RedisKeyPrefix.ETF_DIVIDEND_FETCH.prefix}$stkCd"
        val ops = redisTemplate.opsForValue()
        if (ops.setIfAbsent(key, FETCH_RUNNING, FETCH_RUNNING_TTL) != true) return

        val failedSaves = try {
            collectEtfDividends(stkCd)
        } catch (e: CancellationException) {
            redisTemplate.delete(key)   // 요청이 끊겨 중단된 것은 실패로 치지 않는다
            throw e
        } catch (e: Exception) {
            log.failure(e) { "ETF 분배금 수집 실패, 저장된 분배금으로 응답: stkCd=$stkCd" }
            ops.set(key, FETCH_FAILED, FETCH_FAILED_TTL)
            return
        }
        // 일부라도 저장하지 못했으면 그날 다시 받을 수 있게 실패로 둔다
        if (failedSaves > 0) ops.set(key, FETCH_FAILED, FETCH_FAILED_TTL) else ops.set(key, FETCH_DONE, untilMidnight())
    }

    private fun untilMidnight(): Duration {
        val now = LocalDateTime.now()
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).coerceAtLeast(Duration.ofSeconds(1))
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
