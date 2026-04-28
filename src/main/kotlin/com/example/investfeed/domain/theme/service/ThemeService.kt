package com.example.investfeed.domain.theme.service

import com.example.investfeed.domain.theme.dto.req.ThemeListReq
import com.example.investfeed.domain.theme.dto.req.ThemeStockListReq
import com.example.investfeed.domain.theme.dto.req.ThemeStockListStreamReq
import com.example.investfeed.domain.theme.dto.res.ThemeListItem
import com.example.investfeed.domain.theme.dto.res.ThemeListRes
import com.example.investfeed.domain.theme.dto.res.ThemeStockListItem
import com.example.investfeed.domain.theme.dto.res.ThemeStockListRes
import com.example.investfeed.kiwoom.stock.client.StockSocketClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStream
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStreamReq
import com.example.investfeed.kiwoom.theme.client.ThemeClient
import com.example.investfeed.kiwoom.theme.dto.req.KiwoomThemeGroupReq
import com.example.investfeed.kiwoom.theme.dto.req.KiwoomThemeGroupStockReq
import org.springframework.stereotype.Service

@Service
class ThemeService(
    private val themeClient: ThemeClient,
    private val stockSocketClient: StockSocketClient
) {
    fun listThemes(
        req: ThemeListReq
    ): ThemeListRes {
        val KiwoomThemeGroupRes = themeClient.themeGroupList(
            KiwoomThemeGroupReq(
                qry_tp = "1",
                date_tp =  req.dateTp,
                flu_pl_amt_tp = req.fluPlAmtTp,
                stex_tp = "3"
            )
        )

        val themeList = mutableListOf<ThemeListItem>()
        if (KiwoomThemeGroupRes.return_code == 0) {
            KiwoomThemeGroupRes.thema_grp?.forEach {
                themeList.add(
                    ThemeListItem(
                        themaGrpCd = it.thema_grp_cd,
                        themaNm = it.thema_nm,
                        fluSig = it.flu_sig,
                        fluRt = it.flu_rt,
                        risingStkNum = it.rising_stk_num,
                        fallStkNum = it.fall_stk_num,
                        dtPrftRt = it.dt_prft_rt,
                        mainStk = it.main_stk,
                    )
                )
            }
        }

        return ThemeListRes(
            themeList = themeList
        )
    }

    fun listStocksByTheme(
        themaGrpCd: String,
        req: ThemeStockListReq
    ): ThemeStockListRes {
        val KiwoomThemeGroupStockRes = themeClient.themeGroupStockList(
            req = KiwoomThemeGroupStockReq(
                date_tp = req.dateTp,
                thema_grp_cd = themaGrpCd,
                stex_tp = "3"
            )
        )

        val themeStockList = mutableListOf<ThemeStockListItem>()
        if (KiwoomThemeGroupStockRes.return_code == 0) {
            KiwoomThemeGroupStockRes.thema_comp_stk?.forEach {
                themeStockList.add(
                    ThemeStockListItem(
                        stkCd = it.stk_cd,
                        stkNm = it.stk_nm,
                        curPrc = it.cur_prc,
                        fluSig = it.flu_sig,
                        predPre = it.pred_pre,
                        fluRt = it.flu_rt,
                        accTrdeQty = it.acc_trde_qty,
                        dtPrftRtN = it.dt_prft_rt_n,
                    )
                )
            }
        }

        return ThemeStockListRes(
            themeStockList = themeStockList
        )
    }

    fun streamThemeStocks(
        req: ThemeStockListStreamReq
    ) {
        stockSocketClient.stockListStream(
            req = KiwoomStockStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    KiwoomStockStream(
                        item = req.items,
                        type = listOf("0B")
                    )
                )
            )
        )
    }
}