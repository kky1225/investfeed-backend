package com.example.investfeed.domain.stock.dto.res

data class StockListItem(
    var stk_cd: String? = null, // 종목코드
    var rank: String? = null, // 현재순위
    var stk_nm: String? = null, // 종목명
    var flu_rt: String? = null, // 등락률
    var cur_prc: String? = null, // 현재가
    var trde_prica: String? = null, // 거래대금
    var nxtEnable: String? = null, // NXT가능여부
)