package com.example.investfeed.kiwoom.us.holding.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomUsHoldingRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var crnc_code: String? = null, // 통화코드 USD
    var tot_evlt_amt: String? = null, // 총평가금액 (USD)
    var tot_prch_amt: String? = null, // 총매입금액 (USD)
    var tot_pl_amt: String? = null, // 총손익금액 (USD)
    var tot_pl_rt: String? = null, // 총수익율(%)
    var tot_evlt_amt_krw: String? = null, // 총평가금액(원)
    var tot_prch_amt_krw: String? = null, // 총매입금액(원)
    var tot_pl_amt_krw: String? = null, // 총손익금액(원)
    var result_list: List<KiwoomUsHoldingItemRes>? = null, // 보유종목 리스트
): KiwoomRes(return_code, return_msg)

data class KiwoomUsHoldingItemRes(
    var stex_nm: String? = null, // 거래소명
    var crnc_code: String? = null, // 통화코드
    var stk_cd: String? = null, // 종목코드 (티커)
    var frgn_stk_nm: String? = null, // 종목명
    var poss_qty: String? = null, // 보유수량
    var sell_alowq: String? = null, // 매도가능수량
    var frgn_stk_book_uv: String? = null, // 매입단가 (USD)
    var now_pric: String? = null, // 현재가 (USD)
    var evlt_amt: String? = null, // 평가금액 (USD)
    var pl_amt: String? = null, // 손익금액 (USD)
    var pl_rt: String? = null, // 손익율(%)
    var evlt_amt_krw: String? = null, // 평가금액(원)
    var pl_amt_krw: String? = null, // 손익금액(원)
    var natn_nm: String? = null, // 국가명
    var exch_rate: String? = null, // 환율
    var frgn_stk_book_uv_krw: String? = null, // 매입단가(원)
    var now_pric_krw: String? = null, // 현재가(원)
    var frgn_stk_book_amt: String? = null, // 매입금액 (USD)
    var frgn_stk_book_amt_krw: String? = null, // 매입금액(원)
)
