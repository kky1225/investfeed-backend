package com.example.investfeed.kiwoom.order.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

/** 계좌별 주문체결내역 상세 응답 (kt00007). qry_tp=4 호출 시 체결된 행만 반환. */
data class KiwoomTradeFillsRes(
    override var return_code: Int,
    override var return_msg: String,
    var acnt_ord_cntr_prps_dtl: List<KiwoomTradeFillItem>? = null,
) : KiwoomRes(return_code, return_msg)

data class KiwoomTradeFillItem(
    var ord_no: String? = null,      // 주문번호
    var stk_cd: String? = null,      // 종목번호 (A 접두사 가능)
    var stk_nm: String? = null,      // 종목명
    var trde_tp: String? = null,     // 매매구분 (예: "시장가", "보통")
    var crd_tp: String? = null,      // 신용구분
    var ord_qty: String? = null,     // 주문수량
    var ord_uv: String? = null,      // 주문단가
    var cnfm_qty: String? = null,    // 확인수량
    var acpt_tp: String? = null,     // 접수구분
    var ord_tm: String? = null,      // 주문시간 (HH:MM:SS)
    var ori_ord: String? = null,     // 원주문
    var io_tp_nm: String? = null,    // 주문구분 (예: "현금매수", "현금매도")
    var cntr_qty: String? = null,    // 체결수량
    var cntr_uv: String? = null,     // 체결단가
    var ord_remnq: String? = null,   // 주문잔량
    var mdfy_cncl: String? = null,   // 정정취소
    var cnfm_tm: String? = null,     // 확인시간
    var dmst_stex_tp: String? = null,// 국내거래소구분
)