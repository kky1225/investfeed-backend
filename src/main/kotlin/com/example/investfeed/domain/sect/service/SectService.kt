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
    fun listSects(
        req: SectListReq
    ): SectListRes {
        val kiwoomSectIndexRes = runBlocking { sectClient.sectIndexList(
            KiwoomSectIndexReq(
                inds_cd = req.indsCd,
            )
        ) }

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