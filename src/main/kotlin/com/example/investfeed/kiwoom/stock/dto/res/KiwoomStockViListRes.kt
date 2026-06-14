package com.example.investfeed.kiwoom.stock.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockViListRes(
    override var return_code: Int = 0,
    override var return_msg: String = "",
    var motn_stk: List<KiwoomStockViItem>? = null,   // 발동종목
) : KiwoomRes(return_code, return_msg)

data class KiwoomStockViItem(
    var stk_cd: String? = null,                // 종목코드
    var stk_nm: String? = null,                // 종목명
    var acc_trde_qty: String? = null,          // 누적거래량
    var motn_pric: String? = null,             // 발동가격
    var dynm_dispty_rt: String? = null,        // 동적괴리율
    var trde_cntr_proc_time: String? = null,   // 매매체결처리시각(발동시각)
    var virelis_time: String? = null,          // VI해제시각
    var viaplc_tp: String? = null,             // VI적용구분(정적/동적/동적+정적)
    var dynm_stdpc: String? = null,            // 동적기준가격
    var static_stdpc: String? = null,          // 정적기준가격
    var static_dispty_rt: String? = null,      // 정적괴리율
    var open_pric_pre_flu_rt: String? = null,  // 시가대비등락률
    var vimotn_cnt: String? = null,            // VI발동횟수
    var stex_tp: String? = null,               // 거래소구분
)
