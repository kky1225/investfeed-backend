package com.example.investfeed.domain.us.rank.service

import com.example.investfeed.domain.us.rank.dto.req.UsRankListReq
import com.example.investfeed.domain.us.rank.dto.req.UsStockStreamReq
import com.example.investfeed.domain.us.rank.dto.res.UsRankListItem
import com.example.investfeed.domain.us.rank.dto.res.UsRankListRes
import com.example.investfeed.domain.us.stock.service.UsEtfLookup
import com.example.investfeed.kiwoom.us.rank.client.UsRankClient
import com.example.investfeed.kiwoom.us.rank.dto.req.KiwoomUsStockTradeValueListReq
import com.example.investfeed.kiwoom.us.rank.dto.req.KiwoomUsStockTradeVolumeListReq
import com.example.investfeed.kiwoom.us.rank.dto.req.KiwoomUsSurgeTradeVolumeListReq
import com.example.investfeed.kiwoom.us.stock.client.UsStockSocketClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStream
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStreamItem
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStreamReq
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class UsRankService(
    private val usRankClient: UsRankClient,
    private val usStockSocketClient: UsStockSocketClient,
    private val usEtfLookup: UsEtfLookup,
) {
    private val log = KotlinLogging.logger {}

    fun listUsRanks(
        req: UsRankListReq
    ): UsRankListRes {
        when (req.type) {
            "0" -> {
                val kiwoomUsStockTradeValueListRes = usRankClient.usStockTradeValueList(
                    req = KiwoomUsStockTradeValueListReq(
                        stex_tp = "0",
                        inds_cd = "",
                        stk_tp = "0",
                        trde_qty_tp = "0",
                        stk_cnd = "0",
                        pric_cnd = "0",
                        trde_prica_cnd = "0"
                    )
                )

                val rows = kiwoomUsStockTradeValueListRes.result_list.orEmpty()
                val etfTickers = usEtfLookup.etfTickers(rows.mapNotNull { it.stk_cd })

                return UsRankListRes(
                    return_code = kiwoomUsStockTradeValueListRes.return_code,
                    return_msg = kiwoomUsStockTradeValueListRes.return_msg,
                    rankList = rows.map {
                        UsRankListItem(
                            stkCd = it.stk_cd,
                            stexTp = it.stex_tp,
                            rank = it.rank,
                            stkNm = usEtfLookup.displayName(it.stk_cd, it.stk_nm, etfTickers),
                            fluRt = it.flu_rt,
                            curPrc = it.cur_prc,
                            trdePrica = it.trde_prica,
                        )
                    }
                )
            }
            "1" -> {
                val kiwoomUsStockTradeVolumeListRes = usRankClient.usStockTradeVolumeList(
                    req = KiwoomUsStockTradeVolumeListReq(
                        stex_tp = "0",
                        inds_cd = "",
                        stk_tp = "0",
                        trde_qty_tp = "0",
                        qry_tp = "0",
                        stk_cnd = "0",
                        pric_cnd = "0",
                        trde_prica_cnd = "0"
                    )
                )

                val rows = kiwoomUsStockTradeVolumeListRes.result_list.orEmpty()
                val etfTickers = usEtfLookup.etfTickers(rows.mapNotNull { it.stk_cd })

                return UsRankListRes(
                    return_code = kiwoomUsStockTradeVolumeListRes.return_code,
                    return_msg = kiwoomUsStockTradeVolumeListRes.return_msg,
                    rankList = rows.map {
                        UsRankListItem(
                            stkCd = it.stk_cd,
                            stexTp = it.stex_tp,
                            rank = it.rank,
                            stkNm = usEtfLookup.displayName(it.stk_cd, it.stk_nm, etfTickers),
                            fluRt = it.flu_rt,
                            curPrc = it.cur_prc,
                            trdePrica = it.acc_trde_qty,
                        )
                    }
                )
            }
            else -> {
                val kiwoomUsSurgeTradeVolumeListRes = usRankClient.usStockSurgeTradeVolumeList(
                    req = KiwoomUsSurgeTradeVolumeListReq(
                        stex_tp = "0",
                        inds_cd = "",
                        tm = "5",
                        stk_tp = "0",
                        stk_cnd = "0",
                        pric_cnd = "0",
                        trde_prica_cnd = "0",
                        trde_qty_tp = "0"
                    )
                )

                val rows = kiwoomUsSurgeTradeVolumeListRes.result_list.orEmpty()
                val etfTickers = usEtfLookup.etfTickers(rows.mapNotNull { it.stk_cd })

                return UsRankListRes(
                    return_code = kiwoomUsSurgeTradeVolumeListRes.return_code,
                    return_msg = kiwoomUsSurgeTradeVolumeListRes.return_msg,
                    rankList = rows.map {
                        UsRankListItem(
                            stkCd = it.stk_cd,
                            stexTp = it.stex_tp,
                            rank = it.rank,
                            stkNm = usEtfLookup.displayName(it.stk_cd, it.stk_nm, etfTickers),
                            fluRt = it.flu_rt,
                            curPrc = it.cur_prc,
                            trdePrica = it.sdnin_rt,
                        )
                    }
                )
            }
        }
    }

    fun streamUsStocks(
        req: UsStockStreamReq
    ) {
        usStockSocketClient.usStockListStream(
            req = KiwoomUsStockStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    KiwoomUsStockStream(
                        item = req.items.map {
                            KiwoomUsStockStreamItem(
                                jmcode = it.stkCd,
                                stex_tp = it.stexTp
                            )
                        },
                        type = listOf("FE")
                    )
                )
            )
        )
    }
}
