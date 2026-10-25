package com.example.investfeed.domain.sect.service

import com.example.investfeed.domain.sect.dto.req.SectListReq
import com.example.investfeed.domain.sect.dto.req.SectListStreamReq
import com.example.investfeed.domain.sect.dto.req.SectStockListReq
import com.example.investfeed.domain.sect.dto.res.SectListItem
import com.example.investfeed.domain.sect.dto.res.SectListRes
import com.example.investfeed.domain.sect.dto.res.SectStockListItem
import com.example.investfeed.domain.sect.dto.res.SectStockListRes
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectIndexReq
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectPriceReq
import com.example.investfeed.kiwoom.socket.KiwoomStreamClient
import com.example.investfeed.kiwoom.socket.dto.StreamEntry
import com.example.investfeed.kiwoom.socket.dto.StreamMarket
import org.springframework.stereotype.Service
import kotlinx.coroutines.runBlocking

@Service
class SectService(
    private val kiwoomStreamClient: KiwoomStreamClient,
    private val sectClient: SectClient,
) {
    companion object {
        // 산업 업종만 남기려고 비업종 코드를 제외한다. 업종 개편으로 생긴 새 코드는 걸러지지 않고 그대로 노출된다.
        private val NON_INDUSTRY_CODES = setOf(
            // 코스피: 종합, 대형/중형/소형주, 제조(005~015 합계), 변동성지수, 코스피고배당50, 코스피배당성장50
            "001", "002", "003", "004", "027", "603", "604", "605",
            // 코스닥: 종합, 제조(115~127 합계), KOSDAQ 100/MID 300/SMALL, 우량·벤처·중견·신성장기업,
            // KOSDAQ 150, 코스닥글로벌지수, F-KOSDAQ150, F-KOSDAQ150인버스
            "101", "106", "138", "139", "140", "142", "143", "144", "145", "150", "151", "160", "165",
        )
    }

    fun listSects(
        req: SectListReq
    ): SectListRes {
        val kiwoomSectIndexRes = runBlocking { sectClient.sectIndexList(
            KiwoomSectIndexReq(
                inds_cd = req.indsCd,
            )
        ) }

        val sectList = if (kiwoomSectIndexRes.return_code == 0) {
            kiwoomSectIndexRes.all_inds_idex.orEmpty()
                .filter { it.stk_cd !in NON_INDUSTRY_CODES }
                .map {
                    SectListItem(
                        stkCd = it.stk_cd,
                        stkNm = it.stk_nm,
                        preSig = it.pre_sig,
                        fluRt = it.flu_rt,
                        curPrc = it.cur_prc,
                        trdeQty = it.trde_qty,
                        trdePrica = it.trde_prica,
                        rising = it.rising,
                        stdns = it.stdns,
                        fall = it.fall,
                        floStkNum = it.flo_stk_num,
                    )
                }
        } else emptyList()

        return SectListRes(
            sectList = sectList
        )
    }

    fun streamSects(
        req: SectListStreamReq
    ) {
        kiwoomStreamClient.register(
            StreamEntry(
                market = StreamMarket.KRX,
                items = req.items,
                types = listOf("0J")
            )
        )
    }

    fun listStocksBySect(
        indsCd: String,
        req: SectStockListReq
    ): SectStockListRes {
        val kiwoomSectPriceRes = runBlocking { sectClient.sectPrice(
            req = KiwoomSectPriceReq(
                mrkt_tp = req.mrktTp,
                inds_cd = indsCd,
                stex_tp = "3"
            )
        ) }

        val sectStockList = mutableListOf<SectStockListItem>()
        if (kiwoomSectPriceRes.return_code == 0) {
            kiwoomSectPriceRes.inds_stkpc?.forEach {
                sectStockList.add(
                    SectStockListItem(
                        stkCd = it.stk_cd,
                        stkNm = it.stk_nm,
                        fluRt = it.flu_rt,
                        curPrc = it.cur_prc,
                        predPreSig = it.pred_pre_sig,
                        nowTrdeQty = it.now_trde_qty
                    )
                )
            }
        }

        return SectStockListRes(
            sectStockList = sectStockList
        )
    }
}