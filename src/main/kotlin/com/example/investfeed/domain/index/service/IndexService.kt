package com.example.investfeed.domain.index.service

import com.example.investfeed.common.util.DateUtil
import com.example.investfeed.domain.index.IndexType
import com.example.investfeed.domain.index.dto.req.IndexDetailReq
import com.example.investfeed.domain.index.dto.res.*
import com.example.investfeed.domain.index.entity.IndexInvestorDaily
import com.example.investfeed.domain.index.repository.IndexInvestorDailyRepository
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
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.Collections.emptyList

@Service
class IndexService(
    private val sectClient: SectClient,
    private val sectChartClient: SectChartClient,
    private val priceClient: PriceClient,
    private val indexInvestorDailyRepository: IndexInvestorDailyRepository
) {
    private val log = KotlinLogging.logger {}
    fun indexList(): IndexListRes? {
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

    fun indexDetail(
        req: IndexDetailReq
    ): IndexDetailRes {
        val chartList: MutableList<IndexChart> = mutableListOf()

        when(req.chart_type) {
            IndexChartType.DAY -> {
                val kiwoomSectChartDayRes = sectChartClient.sectChartDayList(
                    req = SectChartDayListReq(
                        inds_cd = req.inds_cd,
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
                        inds_cd = req.inds_cd,
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
                        inds_cd = req.inds_cd,
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
                        inds_cd = req.inds_cd,
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
                            inds_cd = req.inds_cd,
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
                inds_cd = req.inds_cd
            )
        )

        val kiwoomSectInvestorRes = sectClient.sectInvestor(
            req = KiwoomSectInvestorReq(
                mrkt_tp = if (req.inds_cd == "101" || req.inds_cd == "150") "1" else "0",
                amt_qty_tp = "0",
                stex_tp = "3"
            )
        )

        val kiwoomProgramTradeRes = priceClient.programTrade(
            req = KiwoomProgramTradeReq(
                date = DateUtil.today("yyyyMMdd"),
                amt_qty_tp = "1",
                mrkt_tp = if (req.inds_cd == "001" || req.inds_cd == "201") "P001_AL01" else "P101_AL02",
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
                    mrkt_tp = if (req.inds_cd == "001" || req.inds_cd == "201") "0" else "1",
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
                mrkt_tp = if (req.inds_cd == "001" || req.inds_cd == "201") "P001_AL01" else "P101_AL02",
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
                indsCd = req.inds_cd,
                indsNm = IndexType.entries.find { it.indsCd == req.inds_cd }?.indsNm,
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
            investorDailyList = getIndexInvestorDailyList(req.inds_cd),
        )
    }

    fun getIndexInvestorDailyList(indsCd: String): List<IndexInvestorDailyItem> {
        val result = mutableListOf<IndexInvestorDailyItem>()

        val mappedIndsCd = when (indsCd) {
            "201" -> "001"
            "150" -> "101"
            else -> indsCd
        }

        try {
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
                        )
                    )
                }
            }
        } catch (e: Exception) {
            log.error { "지수 당일 투자자 데이터 조회 실패: ${e.message}" }
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

                    val latest = indexInvestorDailyRepository.findFirstByIndsCdOrderByDtDesc(indexType.indsCd)
                    if (latest != null && latest.indNetprps == indNetprps && latest.frgnrNetprps == frgnrNetprps && latest.orgnNetprps == orgnNetprps) {
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
                        )
                    )
                }

                Thread.sleep(100)
            } catch (e: Exception) {
                log.error { "지수 투자자 일별 데이터 수집 실패: indsCd=${indexType.indsCd}, date=$date, ${e.message}" }
            }
        }
    }

}