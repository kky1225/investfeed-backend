package com.example.investfeed.kiwoom.stock.dto.res

data class StockInfo(
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var per: String? = null, // PER
    var eps: String? = null, // EPS
    var roe: String? = null, // ROE
    var pbr: String? = null, // PBR
    var _250hgst: String? = null, // 250최고
    var _250lwst: String? = null, // 250최저
    var high_pric: String? = null, // 고가
    var open_pric: String? = null, // 시가
    var low_pric: String? = null, // 저가
    var cur_prc: String? = null, // 현재가
    var pre_sig: String? = null, // 대비기호
    var flu_rt: String? = null, // 등락율
    var trde_qty: String? = null, // 거래량
    val tm: String? = null, // 시간
    val trde_prica: String? = null, // 누적거래대금
)