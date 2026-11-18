package com.example.investfeed.kiwoom.order.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

/** 위탁종합거래내역 응답 (kt00015). 쓰는 필드만 받는다. */
data class KiwoomTradeHistoryRes(
    override var return_code: Int,
    override var return_msg: String,
    var trst_ovrl_trde_prps_array: List<KiwoomTradeHistoryItem>? = null,
) : KiwoomRes(return_code, return_msg)

/**
 * 거래 한 줄. 한 주문이 나뉘어 체결되면 여러 줄로 온다.
 * trde_dt(거래일자)는 체결일이 아니라 결제일이고, proc_tm(처리시간)은 밤사이 일괄 처리 시각이라 받지 않는다(2026-11-18 실측).
 */
data class KiwoomTradeHistoryItem(
    var cntr_dt: String? = null,            // 체결일 YYYYMMDD
    var stk_cd: String? = null,             // 종목코드 (A005930 처럼 접두어가 붙어 온다)
    var io_tp: String? = null,              // 1:매도, 2:매수
    var trde_qty_jwa_cnt: String? = null,   // 거래수량
    var trde_unit: String? = null,          // 거래단가 (71,000 처럼 쉼표가 들어 있다)
)
