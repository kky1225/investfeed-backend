package com.example.investfeed.domain.recommend.service

import com.example.investfeed.domain.recommend.dto.req.RecommendListStreamReq
import com.example.investfeed.domain.recommend.dto.res.RecommendListItem
import com.example.investfeed.domain.recommend.dto.res.RecommendListRes
import com.example.investfeed.domain.recommend.entity.StockAvoid
import com.example.investfeed.domain.recommend.entity.StockRecommend
import com.example.investfeed.domain.recommend.repository.StockAvoidRepository
import com.example.investfeed.domain.recommend.repository.StockRecommendRepository
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomInvestorTradeCloseMarketReq
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeCloseMarketItemList
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.client.StockSocketClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInvestorReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStream
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStreamReq
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.String

@Service
class RecommendService(
    private val priceClient: PriceClient,
    private val stockClient: StockClient,
    private val stockSocketClient: StockSocketClient,
    private val stockRecommendRepository: StockRecommendRepository,
    private val stockAvoidRepository: StockAvoidRepository
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 30 20 * * *")
    @Transactional
    fun recommandList() {
        val kiwoomInvestorTradeCloseMarketRes = priceClient.investorTradeCloseMarket(
            req = KiwoomInvestorTradeCloseMarketReq(
                mrkt_tp = "000",
                amt_qty_tp = "1",
                trde_tp = "0",
                stex_tp = "3"
            )
        )

        var frgnrList: MutableList<KiwoomInvestorTradeCloseMarketItemList> = mutableListOf()
        var penfndList: MutableList<KiwoomInvestorTradeCloseMarketItemList> = mutableListOf()
        var result: MutableList<KiwoomInvestorTradeCloseMarketItemList> = mutableListOf()
        var recommendResult: MutableList<KiwoomInvestorTradeCloseMarketItemList> = mutableListOf()
        var avoidResult: MutableList<KiwoomInvestorTradeCloseMarketItemList> = mutableListOf()

        if (kiwoomInvestorTradeCloseMarketRes.return_code == 0) {
            // 외국인 순매수금액 top 100
            kiwoomInvestorTradeCloseMarketRes.opaf_invsr_trde?.sortedByDescending { it.frgnr_invsr?.toLongOrNull() ?: 0L }?.stream()?.limit(100)?.let { frgnrList = it.toList() }
            // 연기금 순매수금액 top 100
            kiwoomInvestorTradeCloseMarketRes.opaf_invsr_trde?.sortedByDescending { it.penfnd_etc?.toLongOrNull() ?: 0L }?.stream()?.limit(100)?.let { penfndList = it.toList() }

            // 외국인/연기금 순매수금액 top 100에 동시에 들어간 주식 추출
            val stkCdSet = frgnrList.map { it.stk_cd }.toSet()
            result = penfndList.filter { it.stk_cd in stkCdSet }.sortedByDescending { (it.frgnr_invsr?.toLongOrNull() ?: 0L) + (it.penfnd_etc?.toLongOrNull() ?: 0L) }.toList().toMutableList()

            // 외국인/연기금 순매수금액 동시 top 100에 들어간 주식의 종목별투자자기관별 조회
            recommendResult = result.filter {
                Thread.sleep(100)

                val kiwoomStockInvestor = stockClient.stockInvestor(
                    req = KiwoomStockInvestorReq(
                        dt = today("yyyyMMdd"),
                        stk_cd = it.stk_cd,
                        amt_qty_tp = "2",
                        trde_tp = "0",
                        unit_tp = "1"
                    )
                )

                if (kiwoomStockInvestor.return_code == 0) {
                    var recommendCnt = 0
                    var isRecommend = false
                    var frgnrCnt = 0
                    var orgnCnt = 0
                    var penfndCnt = 0

                    // 최근 5일간 외국인/기관/연기금 순매수 일수 조회
                    kiwoomStockInvestor.stk_invsr_orgn?.take(5)?.forEach { invsr ->
                        val frgnr = invsr.frgnr_invsr?.toLongOrNull() ?: 0
                        val orgn = invsr.orgn?.toLongOrNull() ?: 0
                        val penfnd = invsr.penfnd_etc?.toLongOrNull() ?: 0

                        log.info { it.stk_cd + ", " + frgnr + ", " + orgn + ", " + penfnd }

                        if (frgnr > 0) frgnrCnt++
                        if (orgn > 0) orgnCnt++
                        if (penfnd > 0) penfndCnt++

                        recommendCnt++

                        if (recommendCnt == 1 && frgnr > 0 && penfnd > 0) isRecommend = true
                    }

                    (frgnrCnt >= 3 && orgnCnt >= 3 && penfndCnt >= 4) && isRecommend
                } else {
                    false
                }
            }.toList().toMutableList()

            // 외국인/연기금 순매수금액 동시 top 100에 들어간 주식의 종목별투자자기관별 조회
            avoidResult = result.filter {
                Thread.sleep(100)

                val kiwoomStockInvestor = stockClient.stockInvestor(
                    req = KiwoomStockInvestorReq(
                        dt = today("yyyyMMdd"),
                        stk_cd = it.stk_cd,
                        amt_qty_tp = "2",
                        trde_tp = "0",
                        unit_tp = "1"
                    )
                )

                if (kiwoomStockInvestor.return_code == 0) {
                    var avoidCnt = 0
                    var isAvoid = false
                    var frgnrCnt = 0
                    var orgnCnt = 0
                    var penfndCnt = 0

                    // 최근 5일간 외국인/기관/연기금 순매수 일수 조회
                    kiwoomStockInvestor.stk_invsr_orgn?.take(5)?.forEach { invsr ->
                        val frgnr = invsr.frgnr_invsr?.toLongOrNull() ?: 0
                        val orgn = invsr.orgn?.toLongOrNull() ?: 0
                        val penfnd = invsr.penfnd_etc?.toLongOrNull() ?: 0

                        if (frgnr > 0) frgnrCnt++
                        if (orgn > 0) orgnCnt++
                        if (penfnd > 0) penfndCnt++

                        avoidCnt++
                        if (avoidCnt == 1 && frgnr < 0 && penfnd < 0) isAvoid = true
                    }

                    (frgnrCnt < 3 && orgnCnt < 3 && penfndCnt < 4) && isAvoid
                } else {
                    false
                }
            }.toList().toMutableList()
        }

        // 기존 데이터 삭제 후 새로 저장
        stockRecommendRepository.deleteAll()
        stockAvoidRepository.deleteAll()

        stockRecommendRepository.saveAll(
            recommendResult.map { item ->
                StockRecommend(
                    stkCd = item.stk_cd ?: "",
                    stkNm = item.stk_nm ?: "",
                    fluRt = item.flu_rt ?: "",
                    curPrc = item.cur_prc ?: "",
                    preSig = item.pre_sig ?: ""
                )
            }
        )

        stockAvoidRepository.saveAll(
            avoidResult.map { item ->
                StockAvoid(
                    stkCd = item.stk_cd ?: "",
                    stkNm = item.stk_nm ?: "",
                    fluRt = item.flu_rt ?: "",
                    curPrc = item.cur_prc ?: "",
                    preSig = item.pre_sig ?: ""
                )
            }
        )

        log.info { "추천 종목 ${recommendResult.size}건, 회피 종목 ${avoidResult.size}건 저장 완료" }
    }

    fun getRecommendList(): RecommendListRes {
        val recommendList = stockRecommendRepository.findAll().map { entity ->
            RecommendListItem(
                stkCd = entity.stkCd,
                stkNm = entity.stkNm,
                fluRt = entity.fluRt,
                curPrc = entity.curPrc,
                preSig = entity.preSig
            )
        }

        val avoidList = stockAvoidRepository.findAll().map { entity ->
            RecommendListItem(
                stkCd = entity.stkCd,
                stkNm = entity.stkNm,
                fluRt = entity.fluRt,
                curPrc = entity.curPrc,
                preSig = entity.preSig
            )
        }

        return RecommendListRes(
            recommendList = recommendList,
            avoidList = avoidList
        )
    }

    fun recommendListStream(
        req: RecommendListStreamReq
    ) {
        stockSocketClient.stockListStream(
            req = KiwoomStockStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    KiwoomStockStream(
                        item = req.items,
                        type = listOf("0J")
                    )
                )
            )
        )
    }

    fun today(
        pattern: String
    ): String {
        val now = LocalDate.now()
        val pattern = DateTimeFormatter.ofPattern(pattern)

        return pattern.format(now)
    }
}
