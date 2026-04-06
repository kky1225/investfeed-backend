package com.example.investfeed.kiwoom.etf.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class EtfTradeDailyListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var cntr_tm: String, // 체결시간
    var cur_prc: String, // 현재가
    var pre_sig: String, // 대비기호
    var pred_pre: String, // 전일대비
    var trde_qty: String, // 거래량
    var etfnetprps_qty_array: List<EtfTradeDailyList> // ETF 순매수수량배열
): KiwoomRes(return_code, return_msg)