package com.example.investfeed.domain.papertrade.service

import com.example.investfeed.domain.index.repository.IndexDailyCloseRepository
import com.example.investfeed.domain.papertrade.dto.res.PaperTradeReportRes
import com.example.investfeed.domain.papertrade.repository.PaperFillRepository
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomDepositReq
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import com.example.investfeed.kiwoom.order.client.MockAccountClient
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectPriceNowReq
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 모의 성과 리포트 — 키움 모의계좌 NAV vs 운용기간 지수수익.
 * 순수 계산은 [PaperTradeReportCalculator].
 */
@Service
class PaperTradeReportService(
    private val mockAccountClient: MockAccountClient,
    private val sectClient: SectClient,
    private val indexDailyCloseRepository: IndexDailyCloseRepository,
    private val paperFillRepository: PaperFillRepository,
    private val calc: PaperTradeReportCalculator,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val START_NAV = 100_000_000L  // 키움 모의 1억 시드(첫 런 고정)
        private const val KOSPI = "001"
        private const val KOSDAQ = "101"
        private val YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @Transactional(readOnly = true)
    fun buildReport(): PaperTradeReportRes {
        val startDate = paperFillRepository.findAll().minByOrNull { it.fillDate }?.fillDate
        if (startDate == null) {
            return PaperTradeReportRes(
                startDate = null, startNav = START_NAV, currentNav = START_NAV,
                totalReturnPct = 0.0, kospiReturnPct = null, kosdaqReturnPct = null,
                blendedBenchmarkPct = 0.0,
            )
        }

        val currentNav: Long? = try {
            val dep = mockAccountClient.deposit(KiwoomDepositReq(qry_tp = "3"))
            val hold = mockAccountClient.holdingList(KiwoomHoldingReq(qry_tp = "1", dmst_stex_tp = "KRX"))
            val cash = parseAmt(dep.ord_alow_amt).takeIf { it > 0 } ?: parseAmt(dep.entr)
            (cash + parseAmt(hold?.tot_evlt_amt)).takeIf { it > 0 }
                ?: run {
                    log.error { "모의계좌 NAV 가 0 이하 — 응답 이상 (cash=$cash, tot_evlt_amt=${hold?.tot_evlt_amt})" }
                    null
                }
        } catch (e: Exception) {
            log.error(e) { "모의계좌 NAV 조회 실패" }
            null
        }
        val totalReturnPct = currentNav?.let { calc.pctReturn(START_NAV.toDouble(), it.toDouble()) }

        val kospi = indexReturnPct(KOSPI, startDate)
        val kosdaq = indexReturnPct(KOSDAQ, startDate)
        val available = listOfNotNull(kospi, kosdaq)
        val blended = if (available.isEmpty()) 0.0 else available.average()  // lot 폐기로 단순평균 근사

        return PaperTradeReportRes(
            startDate = startDate,
            startNav = START_NAV,
            currentNav = currentNav,
            totalReturnPct = totalReturnPct,
            kospiReturnPct = kospi,
            kosdaqReturnPct = kosdaq,
            blendedBenchmarkPct = blended,
        )
    }

    private fun indexReturnPct(indsCd: String, startDate: LocalDate): Double? {
        val startRow = indexDailyCloseRepository
            .findFirstByIndsCdAndDtGreaterThanEqualOrderByDtAsc(indsCd, startDate.format(YYYYMMDD))
            ?: return null
        val startPrice = (startRow.openPrice?.toDouble() ?: startRow.closePrice.toDouble())
            .let { it / 100.0 }

        val mrktTp = if (indsCd == KOSDAQ) "1" else "0"
        val rawCurPrc = runCatching {
            val orig = SecurityContextHolder.getContext().authentication
            try {
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
                sectClient.sectPriceNow(KiwoomSectPriceNowReq(mrkt_tp = mrktTp, inds_cd = indsCd)).cur_prc
            } finally {
                SecurityContextHolder.getContext().authentication = orig
            }
        }.onFailure {
            log.warn(it) { "sectPriceNow 호출 실패 inds_cd=$indsCd — DB 최신값으로 폴백" }
        }.getOrNull()

        val latest = rawCurPrc?.replace(Regex("[^0-9.]"), "")?.toBigDecimalOrNull()?.toDouble()
            ?: indexDailyCloseRepository.findFirstByIndsCdOrderByDtDesc(indsCd)?.closePrice?.toDouble()?.let { it / 100.0 }
            ?: return null

        return calc.pctReturn(startPrice, latest)
    }

    private fun parseAmt(raw: String?): Long {
        val v = raw?.replace(Regex("[^0-9-]"), "")?.toLongOrNull() ?: return 0L
        return if (v < 0) -v else v
    }

}
