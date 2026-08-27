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

        // 키움 모의계좌 NAV
        val currentNav = try {
            val dep = mockAccountClient.deposit(KiwoomDepositReq(qry_tp = "3"))
            val hold = mockAccountClient.holdingList(KiwoomHoldingReq(qry_tp = "1", dmst_stex_tp = "KRX"))
            val cash = parseAmt(dep.ord_alow_amt).takeIf { it > 0 } ?: parseAmt(dep.entr)
            (cash + parseAmt(hold?.tot_evlt_amt)).takeIf { it > 0 } ?: START_NAV
        } catch (e: Exception) {
            log.error(e) { "모의계좌 NAV 조회 실패 — startNav 로 대체(수익 0 표기)" }
            START_NAV
        }
        val totalReturnPct = calc.pctReturn(START_NAV.toDouble(), currentNav.toDouble())

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

    /**
     * 누적 등락률(%) — (최신가 − 시작시가) / 시작시가 × 100.
     *
     * **시작점은 startDate 의 시가(open) 사용** — 모의매매가 09:00 시초가 매수로 시작하므로
     * 시가 기준이 NAV 시작 시점(1억 보유 직전)과 정합. close 를 쓰면 startDate 당일 등락이
     * startClose 에 흡수되어 비교에서 누락됨.
     *
     * **단위·부호 정규화 확인됨(2026-05-20)**:
     *  - DB `index_daily_close.open_price`/`close_price` = sectChartDayList 응답 그대로 = **×100 정수**(예: 727166 = 7271.66)
     *  - 실시간 `sectPriceNow.cur_prc` = **소수**(예: "-7137.56") + **전일대비 부호 prefix**(절댓값 사용)
     *  - 부호는 pred_pre_sig(키움 코드 1상한/2상승/3보합/4하한/5하락)가 별도 제공하므로 cur_prc 부호 prefix 는 무시.
     *  → DB ÷ 100 으로 실제 지수값(소수) 단위로 통일, cur_prc 는 절댓값.
     *
     * @KiwoomToken(실거래) 사용 위해 컨텍스트 임시 전환(super 키).
     */
    private fun indexReturnPct(indsCd: String, startDate: LocalDate): Double? {
        // 시작 시가(open) 기준 — 없으면 close 폴백(예전 row 호환). DB ×100 정수 → 실제 지수값(소수)로 단위 통일.
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

        // cur_prc 부호 prefix(+/-) 제거 후 절댓값 — 지수는 음수일 수 없음(전일대비 부호 표기일 뿐).
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
