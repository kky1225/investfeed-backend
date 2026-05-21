package com.example.investfeed.domain.index.service

import com.example.investfeed.common.util.DateUtil
import com.example.investfeed.domain.index.IndexType
import com.example.investfeed.domain.index.dto.req.IndexDetailReq
import com.example.investfeed.domain.index.dto.res.*
import com.example.investfeed.domain.index.entity.IndexDailyClose
import com.example.investfeed.domain.index.entity.IndexInvestorDaily
import com.example.investfeed.domain.index.repository.IndexDailyCloseRepository
import com.example.investfeed.domain.index.repository.IndexInvestorDailyRepository
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.chart.client.SectChartClient
import com.example.investfeed.kiwoom.chart.dto.sect.req.*
import com.example.investfeed.kiwoom.chart.enum.IndexChartType
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomIndexProgramTradeDayReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomIndexProgramTradeMinuteReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomProgramTradeReq
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectPriceNowReq
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Collections.emptyList

@Service
class IndexService(
    private val sectClient: SectClient,
    private val sectChartClient: SectChartClient,
    private val priceClient: PriceClient,
    private val indexInvestorDailyRepository: IndexInvestorDailyRepository,
    private val indexDailyCloseRepository: IndexDailyCloseRepository,
    private val schedulerLogService: SchedulerLogService,
    private val authClient: AuthClient,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    companion object {
        private const val KOSPI_CD = "001"   // 종합(KOSPI)
        private const val KOSDAQ_CD = "101"  // 종합(KOSDAQ)
        private const val INDEX_HISTORY_LIMIT = 100  // 최근 N평일치만 upsert (벤치마크 lookback)
        private const val INDEX_PACING_MS = 100L     // 호출 페이싱
        private val INDEX_DAILY_CLOSE_YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    private val log = KotlinLogging.logger {}
    fun listIndexes(): IndexListRes? {
        val indexTypeList = IndexType.entries
        val indexList: MutableList<IndexListItem> = mutableListOf()

        indexTypeList.forEach { it ->
            val kiwoomSectPriceNowRes = sectClient.sectPriceNow(
                req = KiwoomSectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = it.indsCd
                )
            )

            if (kiwoomSectPriceNowRes.return_code == 0) {
                var chartMinuteList: List<ChartMinute> = mutableListOf()

                val kiwoomSectChartMinuteRes = sectChartClient.sectChartMinuteList(
                    req = SectChartMinuteListReq(
                        inds_cd = it.indsCd,
                        tic_scope = "1"
                    )
                )

                if (kiwoomSectChartMinuteRes.return_code == 0) {
                    chartMinuteList = kiwoomSectChartMinuteRes.inds_min_pole_qry?.map { it ->
                        ChartMinute(
                            curPrc = it.cur_prc,
                            cntrTm = it.cntr_tm
                        )
                    } ?: emptyList()
                }

                indexList.add(
                    IndexListItem(
                        indsCd = it.indsCd,
                        indsNm = it.indsNm,
                        curPrc = kiwoomSectPriceNowRes.cur_prc,
                        predPreSig = kiwoomSectPriceNowRes.pred_pre_sig,
                        predPre = kiwoomSectPriceNowRes.pred_pre,
                        fluRt = kiwoomSectPriceNowRes.flu_rt,
                        trdeQty = kiwoomSectPriceNowRes.trde_qty,
                        trdePrica = kiwoomSectPriceNowRes.trde_prica,
                        openPric = kiwoomSectPriceNowRes.open_pric,
                        tm = kiwoomSectPriceNowRes.inds_cur_prc_tm?.first()?.tm_n,
                        chartMinuteList = chartMinuteList
                    )
                )
            }
        }

        return IndexListRes(
            indexList = indexList
        )
    }

    fun getIndex(
        indsCd: String,
        req: IndexDetailReq
    ): IndexDetailRes {
        val chartList: MutableList<IndexChart> = mutableListOf()

        when(req.chart_type) {
            IndexChartType.DAY -> {
                val kiwoomSectChartDayRes = sectChartClient.sectChartDayList(
                    req = SectChartDayListReq(
                        inds_cd = indsCd,
                        base_dt = DateUtil.today("yyyyMMdd")
                    )
                )

                if (kiwoomSectChartDayRes.return_code == 0) {
                    kiwoomSectChartDayRes.inds_dt_pole_qry?.forEach {
                        chartList.add(
                            IndexChart(
                                dt = it.dt,
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica
                            )
                        )
                    }
                }
            }
            IndexChartType.WEEK -> {
                val kiwoomSectChartWeekRes = sectChartClient.sectChartWeekList(
                    req = SectChartWeekListReq(
                        inds_cd = indsCd,
                        base_dt = DateUtil.today("yyyyMMdd")
                    )
                )

                if (kiwoomSectChartWeekRes.return_code == 0) {
                    kiwoomSectChartWeekRes.inds_stk_pole_qry?.forEach {
                        chartList.add(
                            IndexChart(
                                dt = it.dt,
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica
                            )
                        )
                    }
                }
            }
            IndexChartType.MONTH -> {
                val kiwoomSectChartMonthRes = sectChartClient.sectChartMonthList(
                    req = SectChartMonthListReq(
                        inds_cd = indsCd,
                        base_dt = DateUtil.today("yyyyMMdd")
                    )
                )

                if (kiwoomSectChartMonthRes.return_code == 0) {
                    kiwoomSectChartMonthRes.inds_mth_pole_qry?.forEach {
                        chartList.add(
                            IndexChart(
                                dt = it.dt,
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica
                            )
                        )
                    }
                }
            }
            IndexChartType.YEAR -> {
                val kiwoomSectChartYearRes = sectChartClient.sectChartYearList(
                    req = SectChartYearListReq(
                        inds_cd = indsCd,
                        base_dt = DateUtil.today("yyyyMMdd")
                    )
                )

                if (kiwoomSectChartYearRes.return_code == 0) {
                    kiwoomSectChartYearRes.inds_yr_pole_qry?.forEach {
                        chartList.add(
                            IndexChart(
                                dt = it.dt,
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica
                            )
                        )
                    }
                }
            }
            else -> {
                req.chart_type.value?.let {
                    val kiwoomSectChartMinuteRes = sectChartClient.sectChartMinuteList(
                        req = SectChartMinuteListReq(
                            inds_cd = indsCd,
                            tic_scope = it
                        )
                    )
                    
                    if (kiwoomSectChartMinuteRes.return_code == 0) {
                        kiwoomSectChartMinuteRes.inds_min_pole_qry?.forEach { it ->
                            chartList.add(
                                IndexChart(
                                    dt = it.cntr_tm,
                                    curPrc = it.cur_prc,
                                    openPric = it.open_pric,
                                    highPric = it.high_pric,
                                    lowPric = it.low_pric,
                                    trdeQty = it.trde_qty,
                                    trdePrica = it.acc_trde_qty
                                )
                            )
                        }
                    }
                }
            }
        }

        val kiwoomSectPriceNowRes = sectClient.sectPriceNow(
            req = KiwoomSectPriceNowReq(
                mrkt_tp = "0",
                inds_cd = indsCd
            )
        )

        val kiwoomSectInvestorRes = sectClient.sectInvestor(
            req = KiwoomSectInvestorReq(
                mrkt_tp = if (indsCd == "101" || indsCd == "150") "1" else "0",
                amt_qty_tp = "0",
                stex_tp = "3"
            )
        )

        val kiwoomProgramTradeRes = priceClient.programTrade(
            req = KiwoomProgramTradeReq(
                date = DateUtil.today("yyyyMMdd"),
                amt_qty_tp = "1",
                mrkt_tp = if (indsCd == "001" || indsCd == "201") "P001_AL01" else "P101_AL02",
                min_tic_tp = "1",
                stex_tp = "3",
            )
        )

        val programList = mutableListOf<ProgramListItem>()

        var index = 99
        if (chartList.size < 100) {
            index = chartList.size - 1
        }

        if (index > 0) {
            val kiwoomIndexProgramTradeDayRes = priceClient.indexProgramTradeDay(
                req = KiwoomIndexProgramTradeDayReq(
                    date = chartList[index].dt,
                    amt_qty_tp = "1",
                    mrkt_tp = if (indsCd == "001" || indsCd == "201") "0" else "1",
                    stex_tp = "3",
                )
            )

            if (kiwoomIndexProgramTradeDayRes.return_code == 0) {
                kiwoomIndexProgramTradeDayRes.prm_trde_acc_trnsn?.forEach {
                    programList.add(
                        ProgramListItem(
                            dt = it.dt,
                            dfrtTrdeTdy = it.dfrt_trde_tdy?.replace("--", "-"),
                            ndiffproTrdeTdy = it.ndiffpro_trde_tdy?.replace("--", "-"),
                            allTdy = it.all_tdy?.replace("--", "-"),
                        )
                    )
                }
            }
        }

        val kiwoomIndexProgramTradeMinuteRes = priceClient.indexProgramTradeMinute(
            req = KiwoomIndexProgramTradeMinuteReq(
                date = DateUtil.today("yyyyMMdd"),
                amt_qty_tp = "1",
                mrkt_tp = if (indsCd == "001" || indsCd == "201") "P001_AL01" else "P101_AL02",
                min_tic_tp = "1",
                stex_tp = "3",
            )
        )

        val programChartList: MutableList<ProgramChart> = mutableListOf()
        if (kiwoomIndexProgramTradeMinuteRes.return_code == 0) {
            kiwoomIndexProgramTradeMinuteRes.prm_trde_trnsn?.forEach {
                programChartList.add(
                    ProgramChart(
                        cntrTm = it.cntr_tm,
                        dfrtTrdeNetprps = it.dfrt_trde_netprps?.replace("--", "-"),
                        ndiffproTrdeNetprps = it.ndiffpro_trde_netprps?.replace("--", "-"),
                        allNetprps = it.all_netprps?.replace("--", "-"),
                    )
                )
            }
        }

        return IndexDetailRes(
            indexInfo = IndexInfo(
                indsCd = indsCd,
                indsNm = IndexType.entries.find { it.indsCd == indsCd }?.indsNm,
                curPrc = kiwoomSectPriceNowRes.cur_prc,
                predPreSig = kiwoomSectPriceNowRes.pred_pre_sig,
                predPre = kiwoomSectPriceNowRes.pred_pre,
                fluRt = kiwoomSectPriceNowRes.flu_rt,
                trdeQty = kiwoomSectPriceNowRes.trde_qty,
                trdePrica = kiwoomSectPriceNowRes.trde_prica,
                highPric = kiwoomSectPriceNowRes.high_pric,
                openPric = kiwoomSectPriceNowRes.open_pric,
                lowPric = kiwoomSectPriceNowRes.low_pric,
                _250hgst = kiwoomSectPriceNowRes._52wk_hgst_pric,
                _250lwst = kiwoomSectPriceNowRes._52wk_lwst_pric,
                tmN = kiwoomSectPriceNowRes.inds_cur_prc_tm?.get(0)?.tm_n,
                indNetprps = kiwoomSectInvestorRes.inds_netprps?.get(0)?.ind_netprps,
                frgnrNetprps = kiwoomSectInvestorRes.inds_netprps?.get(0)?.frgnr_netprps,
                orgnNetprps = kiwoomSectInvestorRes.inds_netprps?.get(0)?.orgn_netprps,
                dfrtTrdeNetprps = kiwoomProgramTradeRes.prm_trde_trnsn?.get(0)?.dfrt_trde_netprps,
                ndiffproTrdeNetprps = kiwoomProgramTradeRes.prm_trde_trnsn?.get(0)?.ndiffpro_trde_netprps,
                allNetprps = kiwoomProgramTradeRes.prm_trde_trnsn?.get(0)?.all_netprps,
            ),
            chartList = chartList,
            programChartList = programChartList.reversed(),
            programList = programList,
            investorDailyList = getIndexInvestorDailyList(indsCd),
        )
    }

    fun getIndexInvestorDailyList(indsCd: String): List<IndexInvestorDailyItem> {
        val result = mutableListOf<IndexInvestorDailyItem>()

        val mappedIndsCd = when (indsCd) {
            "201" -> "001"
            "150" -> "101"
            else -> indsCd
        }

        val mrktTp = if (mappedIndsCd == "101") "1" else "0"
        val res = sectClient.sectInvestor(
            req = KiwoomSectInvestorReq(
                mrkt_tp = mrktTp,
                amt_qty_tp = "0",
                stex_tp = "3"
            )
        )

        val investor = res.inds_netprps?.find {
            it.inds_cd.replace("_AL", "") == mappedIndsCd
        }

        if (investor != null) {
            val indNetprps = investor.ind_netprps.replace("--", "-")
            val frgnrNetprps = investor.frgnr_netprps.replace("--", "-")
            val orgnNetprps = investor.orgn_netprps.replace("--", "-")

            val latest = indexInvestorDailyRepository.findFirstByIndsCdOrderByDtDesc(mappedIndsCd)
            val isDuplicate = latest != null && latest.indNetprps == indNetprps && latest.frgnrNetprps == frgnrNetprps && latest.orgnNetprps == orgnNetprps

            if (!isDuplicate) {
                result.add(
                    IndexInvestorDailyItem(
                        dt = DateUtil.today("yyyyMMdd"),
                        indNetprps = indNetprps,
                        frgnrNetprps = frgnrNetprps,
                        orgnNetprps = orgnNetprps,
                        scNetprps = investor.sc_netprps.replace("--", "-"),
                        insrncNetprps = investor.insrnc_netprps.replace("--", "-"),
                        invtrtNetprps = investor.invtrt_netprps.replace("--", "-"),
                        bankNetprps = investor.bank_netprps.replace("--", "-"),
                        endwNetprps = investor.endw_netprps.replace("--", "-"),
                        etcCorpNetprps = investor.etc_corp_netprps.replace("--", "-"),
                        samoFundNetprps = investor.samo_fund_netprps.replace("--", "-"),
                        natnNetprps = investor.natn_netprps.replace("--", "-"),
                        jnsinkmNetprps = investor.jnsinkm_netprps.replace("--", "-"),
                        nativeTrmtFrgnrNetprps = investor.native_trmt_frgnr_netprps.replace("--", "-"),
                    )
                )
            }
        }

        // DB 일별 데이터
        result.addAll(
            indexInvestorDailyRepository
                .findByIndsCdOrderByDtDesc(mappedIndsCd, PageRequest.of(0, 100))
                .map {
                    IndexInvestorDailyItem(
                        dt = it.dt,
                        indNetprps = it.indNetprps,
                        frgnrNetprps = it.frgnrNetprps,
                        orgnNetprps = it.orgnNetprps,
                        scNetprps = it.scNetprps,
                        insrncNetprps = it.insrncNetprps,
                        invtrtNetprps = it.invtrtNetprps,
                        bankNetprps = it.bankNetprps,
                        endwNetprps = it.endwNetprps,
                        etcCorpNetprps = it.etcCorpNetprps,
                        samoFundNetprps = it.samoFundNetprps,
                        natnNetprps = it.natnNetprps,
                        jnsinkmNetprps = it.jnsinkmNetprps,
                        nativeTrmtFrgnrNetprps = it.nativeTrmtFrgnrNetprps,
                    )
                }
        )

        return result
    }

    fun collectIndexInvestorDaily(date: String) {
        val targetIndexes = listOf(IndexType.KOSPI, IndexType.KOSDAQ)

        targetIndexes.forEach { indexType ->
            try {
                if (indexInvestorDailyRepository.existsByIndsCdAndDt(indexType.indsCd, date)) {
                    return@forEach
                }

                val mrktTp = if (indexType.indsCd == "101") "1" else "0"
                val res = sectClient.sectInvestor(
                    req = KiwoomSectInvestorReq(
                        mrkt_tp = mrktTp,
                        amt_qty_tp = "0",
                        base_dt = date,
                        stex_tp = "3"
                    )
                )

                val investor = res.inds_netprps?.find {
                    it.inds_cd.replace("_AL", "") == indexType.indsCd
                }

                if (investor != null) {
                    val indNetprps = investor.ind_netprps.replace("--", "-")
                    val frgnrNetprps = investor.frgnr_netprps.replace("--", "-")
                    val orgnNetprps = investor.orgn_netprps.replace("--", "-")
                    val scNetprps = investor.sc_netprps.replace("--", "-")
                    val insrncNetprps = investor.insrnc_netprps.replace("--", "-")
                    val invtrtNetprps = investor.invtrt_netprps.replace("--", "-")
                    val bankNetprps = investor.bank_netprps.replace("--", "-")
                    val endwNetprps = investor.endw_netprps.replace("--", "-")
                    val etcCorpNetprps = investor.etc_corp_netprps.replace("--", "-")
                    val samoFundNetprps = investor.samo_fund_netprps.replace("--", "-")
                    val natnNetprps = investor.natn_netprps.replace("--", "-")
                    val jnsinkmNetprps = investor.jnsinkm_netprps.replace("--", "-")
                    val nativeTrmtFrgnrNetprps = investor.native_trmt_frgnr_netprps.replace("--", "-")

                    val latest = indexInvestorDailyRepository.findFirstByIndsCdOrderByDtDesc(indexType.indsCd)
                    if (latest != null && latest.dt == date && latest.indNetprps == indNetprps && latest.frgnrNetprps == frgnrNetprps && latest.orgnNetprps == orgnNetprps) {
                        return@forEach
                    }

                    indexInvestorDailyRepository.save(
                        IndexInvestorDaily(
                            indsCd = indexType.indsCd,
                            dt = date,
                            indNetprps = indNetprps,
                            frgnrNetprps = frgnrNetprps,
                            orgnNetprps = orgnNetprps,
                            scNetprps = scNetprps,
                            insrncNetprps = insrncNetprps,
                            invtrtNetprps = invtrtNetprps,
                            bankNetprps = bankNetprps,
                            endwNetprps = endwNetprps,
                            etcCorpNetprps = etcCorpNetprps,
                            samoFundNetprps = samoFundNetprps,
                            natnNetprps = natnNetprps,
                            jnsinkmNetprps = jnsinkmNetprps,
                            nativeTrmtFrgnrNetprps = nativeTrmtFrgnrNetprps,
                        )
                    )
                }

                Thread.sleep(100)
            } catch (e: Exception) {
                log.warn { "지수 투자자 일별 데이터 수집 실패: indsCd=${indexType.indsCd}, date=$date, ${e.message}" }
            }
        }
    }

    // ── 벤치마크 지수 일봉(open/close) 수집 ─────────────────────────────────
    // IndexDailyCloseScheduler(00:10) 또는 ManualTriggerService 가 호출.
    // 진입 가드(휴장일/멱등성)는 호출자에서 처리. 본 서비스는 단순 실행.

    /** 지수 일봉(open/close) 수집 본체. schedulerLogService 로 wrapping. */
    fun runCollectIndexClose() {
        schedulerLogService.execute(SchedulerName.IndexDailyCloseScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                collectIndexClose()
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    /**
     * 벤치마크용 코스피(001)/코스닥(101) 지수 종가·시가 수집 → index_daily_close upsert.
     *
     * sectChartDayList 가 base_dt 기준 일봉 *시계열*을 반환하므로 지수당 1콜로 과거치까지 확보.
     * (inds_cd, dt) 이미 있으면 skip(불변 과거값이라 갱신 불필요) → 일일 수집·과거 backfill 통합.
     * 지수 값 스케일은 키움 제공 형식 그대로 저장 — 벤치마크는 *비율* 비교라 스케일 불변.
     */
    private fun collectIndexClose() {
        val baseDt = LocalDate.now().format(INDEX_DAILY_CLOSE_YYYYMMDD)
        var inserted = 0
        for ((idx, indsCd) in listOf(KOSPI_CD, KOSDAQ_CD).withIndex()) {
            if (idx > 0) Thread.sleep(INDEX_PACING_MS)
            try {
                val res = sectChartClient.sectChartDayList(SectChartDayListReq(inds_cd = indsCd, base_dt = baseDt))
                val rows = res.inds_dt_pole_qry ?: continue
                for (row in rows.take(INDEX_HISTORY_LIMIT)) {
                    val dt = row.dt ?: continue
                    // 당일(=base_dt) row 는 정산 전이라 open/close 부정확 → 다음 사이클에서 저장.
                    if (dt == baseDt) continue
                    if (indexDailyCloseRepository.existsByIndsCdAndDt(indsCd, dt)) continue
                    val close = parseIndexValue(row.cur_prc) ?: continue
                    indexDailyCloseRepository.save(
                        IndexDailyClose(
                            indsCd = indsCd,
                            dt = dt,
                            closePrice = close,
                            openPrice = parseIndexValue(row.open_pric),
                        )
                    )
                    inserted++
                }
            } catch (e: Exception) {
                log.error(e) { "지수 종가 수집 실패 inds_cd=$indsCd" }
            }
        }
        log.info { "index_daily_close 수집 완료 — base_dt=$baseDt, 신규 적재=$inserted" }
    }

    /** 키움 숫자 문자열(부호 prefix 가능) → 절대값 BigDecimal. 파싱 실패 시 null. */
    private fun parseIndexValue(raw: String?): BigDecimal? {
        val cleaned = raw?.replace(Regex("[^0-9.\\-]"), "") ?: return null
        return cleaned.toBigDecimalOrNull()?.abs()
    }

    /**
     * 서버 시작 시 지수 종가 히스토리 backfill (로컬 환경용, 형제 IndexInvestorDailyScheduler 패턴).
     * cron 경로 밖이라 00:10 잡 시간 무관. boot 실패해도 앱 기동에 영향 없음(runCatching).
     */
    @EventListener(ApplicationReadyEvent::class)
    fun backfillIndexCloseOnStartup() {
        runCatching {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                collectIndexClose()
            } finally {
                SecurityContextHolder.clearContext()
            }
        }.onFailure { log.warn(it) { "index_daily_close 기동 backfill 실패 (무시)" } }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

}