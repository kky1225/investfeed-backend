package com.example.investfeed.kiwoom.us.stock.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomUsStockInfoRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var stex_tp: String? = null, // 거래소구분 NA:AMEX, ND:NASDAQ, NY:NYSE
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var stk_enm: String? = null, // 종목영문명
    var cur_prc: String? = null, // 현재가 (USD)
    var pred_pre_sig: String? = null, // 전일대비기호 1:상한가, 2:상승, 3:보합, 4:하한가, 5:하락
    var pred_pre: String? = null, // 전일대비 (USD)
    var flu_rt: String? = null, // 등락률 (%)
    var acc_trde_qty: String? = null, // 누적거래량 (1주)
    var base_exrt: String? = null, // 환율
): KiwoomRes(return_code, return_msg)
