package com.example.investfeed.domain.holding.service

import com.example.investfeed.common.util.MarketTimeUtil.isKrxHoldingClose
import com.example.investfeed.domain.holding.dto.req.HoldingStreamReq
import com.example.investfeed.domain.holding.dto.res.HoldingItem
import com.example.investfeed.domain.holding.dto.res.HoldingListRes
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.kiwoom.holding.client.HoldingClient
import com.example.investfeed.kiwoom.holding.client.HoldingSocketClient
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingStreamReq
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class HoldingService(
    private val holdingClient: HoldingClient,
    private val holdingSocketClient: HoldingSocketClient,
    private val memberHoldingSyncService: MemberHoldingSyncService,
    private val brokerRepository: BrokerRepository,
    private val memberHoldingRepository: MemberHoldingRepository,
) {
    fun holdingList(): HoldingListRes {
        val res = holdingClient.holdingList(
            req = KiwoomHoldingReq(
                qry_tp = "1",
                dmst_stex_tp = if(isKrxHoldingClose()) "NXT" else "KRX"
            )
        )

        val holdingList = res?.acnt_evlt_remn_indv_tot?.map { stock ->
            HoldingItem(
                stkCd = (stock.stk_cd?.removePrefix("A") ?: "") + "_AL",
                stkNm = stock.stk_nm ?: "",
                curPrc = stock.cur_prc?.replace("^[+-]".toRegex(), "") ?: "0",
                purPric = stock.pur_pric ?: "0",
                purAmt = stock.pur_amt ?: "0",
                evltAmt = stock.evlt_amt ?: "0",
                evltvPrft = stock.evltv_prft ?: "0",
                prftRt = stock.prft_rt ?: "0",
                rmndQty = stock.rmnd_qty ?: "0",
                possRt = stock.poss_rt ?: "0",
                predClosePric = stock.pred_close_pric?.replace("^[+-]".toRegex(), "") ?: "0",
            )
        } ?: emptyList()

        val memberId = getMemberId()
        var sortedHoldingList = holdingList
        if (memberId != null && holdingList.isNotEmpty()) {
            val kiwoomBroker = brokerRepository.findByName("키움증권")
            if (kiwoomBroker != null) {
                memberHoldingSyncService.sync(
                    memberId = memberId,
                    holdings = holdingList.map { it.stkCd to it.stkNm },
                    broker = kiwoomBroker
                )

                val memberHoldings = memberHoldingRepository.findByMemberIdAndBrokerIdOrderByDisplayOrderAsc(memberId, kiwoomBroker.id)
                val holdingMap = holdingList.associateBy { it.stkCd }
                sortedHoldingList = memberHoldings.mapNotNull { mh ->
                    holdingMap[mh.stkCd]?.copy(id = mh.id)
                }
            }
        }

        return HoldingListRes(
            totPurAmt = res?.tot_pur_amt ?: "0",
            totEvltAmt = res?.tot_evlt_amt ?: "0",
            totEvltPl = res?.tot_evlt_pl ?: "0",
            totPrftRt = res?.tot_prft_rt ?: "0",
            holdingList = sortedHoldingList
        )
    }

    fun holdingStream(req: HoldingStreamReq) {
        holdingSocketClient.holdingStream(
            req = KiwoomHoldingStreamReq(items = req.items)
        )
    }

    private fun getMemberId(): Long? {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id
    }
}
