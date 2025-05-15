package com.example.investfeed.kiwoom.stock.dto.res

data class StockInfoJumpList(
    var stk_cd: String? = null, // 종목코드
    var stk_cls: String? = null, // 종목분류
    var stk_nm: String? = null, // 종목명
    var pred_pre_sig: String? = null, // 전일대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락률
    var base_pric: String? = null, // 기준가
    var cur_prc: String? = null, // 현재가
    var base_pre: String? = null, // 기준대비
    var trde_qty: String? = null, // 거래량
    var jmp_rt: String? = null // 급등률
)