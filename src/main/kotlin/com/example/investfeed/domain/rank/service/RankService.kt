package com.example.investfeed.domain.rank.service

import com.example.investfeed.domain.rank.dto.req.RankListReq
import com.example.investfeed.domain.rank.dto.res.RankListItem
import com.example.investfeed.domain.rank.dto.res.RankListRes
import com.example.investfeed.kiwoom.rank.client.RankClient
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomStockTradeValueListReq
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomStockTradeVolumeListReq
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomSurgeTradeVolumeListReq
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class RankService(
    private val rankClient: RankClient,
) {
    private val log = KotlinLogging.logger {}

    fun rankList(
        req: RankListReq
    ): RankListRes {
        when (req.type) {
            "0" -> {
                val kiwoomStockTradeValueListRes = rankClient.stockTradeValueList(
                    req = KiwoomStockTradeValueListReq(
                        mrkt_tp = "000",
                        mang_stk_incls = "1",
                        stex_tp = "3"
                    )
                )

                return RankListRes(
                    return_code = kiwoomStockTradeValueListRes.return_code,
                    return_msg = kiwoomStockTradeValueListRes.return_msg,
                    rankList = kiwoomStockTradeValueListRes.trde_prica_upper?.map {
                        RankListItem(
                            stkCd = it.stk_cd,
                            rank = it.now_rank,
                            stkNm = it.stk_nm,
                            fluRt = it.flu_rt,
                            curPrc = it.cur_prc,
                            trdePrica = it.trde_prica,
                        )
                    } ?: emptyList()
                )
            }
            "1" -> {
                val kiwoomStockTradeVolumeListRes = rankClient.stockTradeVolumeList(
                    req = KiwoomStockTradeVolumeListReq(
                        mrkt_tp = "000",
                        sort_tp = "1",
                        mang_stk_incls = "0",
                        crd_tp = "0",
                        trde_qty_tp = "0",
                        pric_tp = "0",
                        trde_prica_tp = "0",
                        mrkt_open_tp = "0",
                        stex_tp = "3"
                    )
                )

                return RankListRes(
                    return_code = kiwoomStockTradeVolumeListRes.return_code,
                    return_msg = kiwoomStockTradeVolumeListRes.return_msg,
                    rankList = kiwoomStockTradeVolumeListRes.tdy_trde_qty_upper?.mapIndexed { index, it ->
                        RankListItem(
                            stkCd = it.stk_cd,
                            rank = (index + 1).toString(),
                            stkNm = it.stk_nm,
                            fluRt = it.flu_rt,
                            curPrc = it.cur_prc,
                            trdePrica = it.trde_qty,
                        )
                    } ?: emptyList()
                )
            }
            else -> {
                val kiwoomStockTradeValueListRes = rankClient.stockSurgeTradeVolumeList(
                    req = KiwoomSurgeTradeVolumeListReq(
                        mrkt_tp = "000",
                        sort_tp = "2",
                        tm_tp = "2",
                        trde_qty_tp = "5",
                        stk_cnd = "0",
                        pric_tp = "0",
                        stex_tp = "3"
                    )
                )

                return RankListRes(
                    return_code = kiwoomStockTradeValueListRes.return_code,
                    return_msg = kiwoomStockTradeValueListRes.return_msg,
                    rankList = kiwoomStockTradeValueListRes.trde_qty_sdnin?.mapIndexed { index, it ->
                        RankListItem(
                            stkCd = it.stk_cd,
                            rank = (index + 1).toString(),
                            stkNm = it.stk_nm,
                            fluRt = it.flu_rt,
                            curPrc = it.cur_prc,
                            trdePrica = it.sdnin_rt,
                        )
                    } ?: emptyList()
                )
            }
        }
    }

    fun time(
        pattern: String
    ): String {
        val now = LocalTime.now()
        val pattern = DateTimeFormatter.ofPattern(pattern)

        return pattern.format(now)
    }
}