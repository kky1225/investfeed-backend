package com.example.investfeed.domain.sect.service

import com.example.investfeed.domain.dashboard.DashboardIndexType
import com.example.investfeed.domain.sect.dto.req.SectListReq
import com.example.investfeed.domain.sect.dto.req.SectListStreamReq
import com.example.investfeed.domain.sect.dto.req.SectStockListReq
import com.example.investfeed.domain.sect.dto.res.SectListItem
import com.example.investfeed.domain.sect.dto.res.SectListRes
import com.example.investfeed.domain.sect.dto.res.SectStockListItem
import com.example.investfeed.domain.sect.dto.res.SectStockListRes
import com.example.investfeed.kiwoom.realtime.client.RealTimeClient
import com.example.investfeed.kiwoom.realtime.dto.SectIndexListStream
import com.example.investfeed.kiwoom.realtime.dto.SectIndexListStreamReq
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectIndexReq
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectPriceReq
import org.springframework.stereotype.Service

@Service
class SectService(
    private val sectClient: SectClient,
    private val realTimeClient: RealTimeClient
) {
    fun sectList(
        req: SectListReq
    ): SectListRes {
        val kiwoomSectIndexRes = sectClient.sectIndexList(
            KiwoomSectIndexReq(
                inds_cd = req.indsCd,
            )
        )

        val sectList = mutableListOf<SectListItem>()
        if (kiwoomSectIndexRes.return_code == 0) {
            kiwoomSectIndexRes.all_inds_idex?.forEach {
                sectList.add(
                    SectListItem(
                        stkCd = it.stk_cd,
                        stkNm = it.stk_nm,
                        preSig = it.pre_sig,
                        fluRt = it.flu_rt,
                        curPrc = it.cur_prc,
                        trdeQty = it.trde_qty,
                        trdePrica = it.trde_prica,
                    )
                )
            }
        }

        return SectListRes(
            sectList = sectList
        )
    }

    fun sectListStream(
        req: SectListStreamReq
    ) {
        realTimeClient.sectIndexListStream(
            req = SectIndexListStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    SectIndexListStream(
                        item = req.items,
                        type = listOf("0J")
                    )
                )
            )
        )
    }

    fun sectStockList(
        req: SectStockListReq
    ): SectStockListRes {
        val kiwoomSectPriceRes = sectClient.sectPrice(
            req = KiwoomSectPriceReq(
                mrkt_tp = req.mrktTp,
                inds_cd = req.indsCd,
                stex_tp = "3"
            )
        )

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