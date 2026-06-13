package com.example.investfeed.domain.papertrade.admin.service

import com.example.investfeed.domain.papertrade.admin.dto.req.AdminHoldingGradeReq
import com.example.investfeed.domain.papertrade.admin.dto.req.AdminPaperRealizedPnlReq
import com.example.investfeed.domain.papertrade.admin.dto.req.AdminPaperTradeHistoryReq
import com.example.investfeed.domain.papertrade.admin.dto.res.AdminHoldingGradeRes
import com.example.investfeed.domain.papertrade.admin.dto.res.AdminPaperAccountRes
import com.example.investfeed.domain.papertrade.admin.dto.res.AdminPaperRealizedPnlRes
import com.example.investfeed.domain.papertrade.admin.dto.res.AdminPaperTradeHistoryRes
import com.example.investfeed.domain.papertrade.repository.HoldingGradeRepository
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomDepositReq
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import com.example.investfeed.kiwoom.order.client.MockAccountClient
import com.example.investfeed.kiwoom.order.dto.req.KiwoomTradeFillsReq
import com.example.investfeed.kiwoom.realizedpnl.dto.req.KiwoomRealizedPnlReq
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 관리자 모의투자 매매 페이지용 데이터 집계.
 * 단일 모의계좌 전제 — 회원/계좌별 격리 없음.
 */
@Service
class AdminPaperTradeService(
    private val mockAccountClient: MockAccountClient,
    private val holdingGradeRepository: HoldingGradeRepository,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    // ── /account ─────────────────────────────────────────────────────────────
    fun getAccount(): AdminPaperAccountRes {
        val dep = mockAccountClient.deposit(KiwoomDepositReq(qry_tp = "3"))
        val hold = mockAccountClient.holdingList(KiwoomHoldingReq(qry_tp = "1", dmst_stex_tp = "KRX"))

        val deposit = parseAmt(dep.entr)
        val orderable = parseAmt(dep.ord_alow_amt).takeIf { it > 0 } ?: deposit
        val totalPur = parseAmt(hold?.tot_pur_amt)
        val totalEvlt = parseAmt(hold?.tot_evlt_amt)
        val totalPl = parseSignedLong(hold?.tot_evlt_pl)
        val totalPrftRt = hold?.tot_prft_rt?.toDoubleOrNull()
        val nav = parseAmt(hold?.prsm_dpst_aset_amt).takeIf { it > 0 } ?: (deposit + totalEvlt)

        val holdings = (hold?.acnt_evlt_remn_indv_tot.orEmpty()).map { h ->
            AdminPaperAccountRes.HoldingItem(
                stkCd = toAlForm(h.stk_cd),
                stkNm = h.stk_nm ?: "-",
                rmndQty = parseAmt(h.rmnd_qty),
                trdeAbleQty = parseAmt(h.trde_able_qty),
                purPric = parseAmt(h.pur_pric),
                curPrc = parseAmt(h.cur_prc),
                purAmt = parseAmt(h.pur_amt),
                evltAmt = parseAmt(h.evlt_amt),
                evltvPrft = parseSignedLong(h.evltv_prft),
                prftRt = h.prft_rt?.toDoubleOrNull(),
                possRt = h.poss_rt?.toDoubleOrNull(),
            )
        }

        return AdminPaperAccountRes(
            summary = AdminPaperAccountRes.AccountSummary(
                deposit = deposit, orderableAmt = orderable,
                totalPurAmt = totalPur, totalEvltAmt = totalEvlt,
                totalEvltPl = totalPl, totalPrftRt = totalPrftRt, nav = nav,
            ),
            holdings = holdings,
        )
    }

    fun getRealizedPnl(req: AdminPaperRealizedPnlReq): AdminPaperRealizedPnlRes {
        val today = LocalDate.now()
        val (from, to) = when (req.viewMode) {
            "monthly" -> {
                val y = req.year ?: today.year
                val m = req.month ?: today.monthValue
                val start = LocalDate.of(y, m, 1)
                start to start.withDayOfMonth(start.lengthOfMonth())
            }
            "yearly" -> {
                val y = req.year ?: today.year
                LocalDate.of(y, 1, 1) to LocalDate.of(y, 12, 31)
            }
            "all" -> LocalDate.of(today.year - 2, 1, 1) to today
            else -> LocalDate.of(today.year, 1, 1) to today
        }
        val res = mockAccountClient.realizedPnl(
            KiwoomRealizedPnlReq(strt_dt = from.format(YYYYMMDD), end_dt = to.format(YYYYMMDD))
        )
        // (year, month) 별 group-by 합계
        val grouped = (res?.dt_rlzt_pl.orEmpty())
            .groupBy { (it.dt ?: "").take(6) }      // YYYYMM
            .mapNotNull { (ym, days) ->
                if (ym.length != 6) return@mapNotNull null
                val y = ym.take(4).toIntOrNull() ?: return@mapNotNull null
                val m = ym.substring(4, 6).toIntOrNull() ?: return@mapNotNull null
                AdminPaperRealizedPnlRes.MonthlyItem(
                    year = y, month = m,
                    realizedPnl = days.sumOf { parseSignedLong(it.tdy_sel_pl) },
                    totalBuyAmt = days.sumOf { parseAmt(it.buy_amt) },
                    totalSellAmt = days.sumOf { parseAmt(it.sell_amt) },
                    tradeFee = days.sumOf { parseAmt(it.tdy_trde_cmsn) },
                    tradeTax = days.sumOf { parseAmt(it.tdy_trde_tax) },
                )
            }
            .sortedWith(compareByDescending<AdminPaperRealizedPnlRes.MonthlyItem> { it.year }.thenByDescending { it.month })
        return AdminPaperRealizedPnlRes(viewMode = req.viewMode, year = req.year, month = req.month, items = grouped)
    }

    // ── /trade-history (kt00007 계좌별 주문체결내역 상세, qry_tp=4) ───────────
    fun getTradeHistory(req: AdminPaperTradeHistoryReq): AdminPaperTradeHistoryRes {
        val ordDtStr = (req.ordDt ?: LocalDate.now()).format(YYYYMMDD)
        val res = mockAccountClient.tradeFills(
            KiwoomTradeFillsReq(
                ord_dt = ordDtStr,
                qry_tp = "4",         // 체결내역만
                stk_bond_tp = "0",
                sell_tp = "0",
                stk_cd = "",
                fr_ord_no = "",
                dmst_stex_tp = "KRX", // 모의는 KRX만 지원
            )
        )
        val items = (res?.acnt_ord_cntr_prps_dtl.orEmpty()).map { r ->
            AdminPaperTradeHistoryRes.TradeItem(
                ordDt = ordDtStr,
                ordTm = r.ord_tm,
                stkCd = toAlForm(r.stk_cd),
                stkNm = r.stk_nm ?: "-",
                ioTpNm = r.io_tp_nm,
                trdeTp = r.trde_tp,
                cntrQty = parseAmt(r.cntr_qty),
                cntrUv = parseAmt(r.cntr_uv),
                ordQty = parseAmt(r.ord_qty),
                ordUv = parseAmt(r.ord_uv),
                ordNo = r.ord_no,
            )
        }
        return AdminPaperTradeHistoryRes(ordDt = ordDtStr, items = items)
    }

    // ── /holding-grade (보유 평가 탭) ────────────────────────────────────────
    /**
     * 22:10 HoldingGradeScheduler 가 저장한 보유 평가 결과 조회 (eval_date 단위).
     * evalDate 미지정 시 가장 최근 평가일자 사용. 데이터 없으면 evalDate=null + 빈 items.
     */
    fun getHoldingGrade(req: AdminHoldingGradeReq): AdminHoldingGradeRes {
        val target = req.evalDate
            ?: holdingGradeRepository.findFirstByOrderByEvalDateDesc()?.evalDate
            ?: return AdminHoldingGradeRes(evalDate = null, items = emptyList())

        val items = holdingGradeRepository.findByEvalDate(target).map { g ->
            AdminHoldingGradeRes.HoldingGradeItem(
                stkCd = toAlForm(g.stkCd),
                stkNm = g.stkNm,
                type = g.type,
                originSide = g.originSide,
                marketType = g.marketType,
                penfndK = g.penfndK,
                frgnrMcapRatio = g.frgnrMcapRatio,
                frgnrOppositeK = g.frgnrOppositeK,
                frgnrSameDirK = g.frgnrSameDirK,
                priorTrendRatio = g.priorTrendRatio,
                foreignerAligned = g.foreignerAligned,
                evaluationReason = g.evaluationReason,
                targetWeightRatio = g.targetWeightRatio,
            )
        }
        return AdminHoldingGradeRes(evalDate = target, items = items)
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private fun toAlForm(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        val bare = raw.substringBefore("_").trimStart('A', 'a')
        return if (bare.isBlank()) "-" else "${bare}_AL"
    }

    /** 키움 숫자 문자열(부호·0패딩) → 절대값 Long. */
    private fun parseAmt(raw: String?): Long {
        val v = raw?.replace(Regex("[^0-9-]"), "")?.toLongOrNull() ?: return 0L
        return if (v < 0) -v else v
    }

    /** 부호 유지 Long (손익 컬럼용). */
    private fun parseSignedLong(raw: String?): Long {
        return raw?.replace(Regex("[^0-9-]"), "")?.toLongOrNull() ?: 0L
    }
}
