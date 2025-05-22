package com.example.investfeed.kiwoom.stock.dto.res

data class StockTradeDailyList (
    var dt: String? = null, // 일자
    var close_pric: String? = null, // 종가
    var pred_pre_sig: String? = null, // 전일대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락율
    var trde_qty: String? = null, // 거래량
    var trde_prica: String? = null, // 거래대금
    var bf_mkrt_trde_qty: String? = null, // 장전거래량
    var bf_mkrt_trde_wght: String? = null, // 장전거래비중
    var opmr_trde_qty: String? = null, // 장중거래량
    var opmr_trde_wght: String? = null, // 장중거래비중
    var af_mkrt_trde_qty: String? = null, // 장후거래량
    var af_mkrt_trde_wght: String? = null, // 장후거래비중
    var tot_3: String? = null, // 합계3
    var prid_trde_qty: String? = null, // 기간중거래량
    var cntr_str: String? = null, // 체결강도
    var for_poss: String? = null, // 외인보유
    var for_wght: String? = null, // 외인비중
    var for_netprps: String? = null, // 외인순매수
    var orgn_netprps: String? = null, // 기관순매수
    var ind_netprps: String? = null, // 개인순매수
    var frgn: String? = null, // 외국계
    var crd_remn_rt: String? = null, // 신용잔고율
    var prm: String? = null, // 프로그램
    var bf_mkrt_trde_prica: String? = null, // 장전거래대금
    var bf_mkrt_trde_prica_wght: String? = null, // 장전거래대금비중
    var opmr_trde_prica: String? = null, // 장중거래대금
    var opmr_trde_prica_wght: String? = null, // 장중거래대금비중
    var af_mkrt_trde_prica: String? = null, // 장후거래대금
    var af_mkrt_trde_prica_wght: String? = null // 장후거래대금비중
)