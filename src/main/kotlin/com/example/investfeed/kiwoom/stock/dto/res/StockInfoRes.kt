package com.example.investfeed.kiwoom.stock.dto.res

data class StockInfoRes(
    var return_code: Int,
    var return_msg: String,
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var setl_mm: String? = null, // 결산월
    var fav: String? = null, // 액면가
    var cap: String? = null, // 자본금
    var flo_stk: String? = null, // 상장주식
    var crd_rt: String? = null, // 신용비율
    var oyr_hgst: String? = null, // 연중최고
    var oyr_lwst: String? = null, // 연중최저
    var mac: String? = null, // 시가총액
    var mac_wght: String? = null, // 시가총액비중
    var for_exh_rt: String? = null, // 외인소진률
    var repl_pric: String? = null, // 대용가
    var per: String? = null, // PER
    var eps: String? = null, // EPS
    var roe: String? = null, // ROE
    var pbr: String? = null, // PBR
    var ev: String? = null, // EV
    var bps: String? = null, // BPS
    var sale_amt: String? = null, // 매출액
    var bus_pro: String? = null, // 영업이익
    var cup_nga: String? = null, // 당기순이익
    var _250hgst: String? = null, // 250최고
    var _250lwst: String? = null, // 250최저
    var high_pric: String? = null, // 고가
    var open_pric: String? = null, // 시가
    var low_pric: String? = null, // 저가
    var upl_pric: String? = null, // 상한가
    var lst_pric: String? = null, // 하한가
    var base_pric: String? = null, // 기준가
    var exp_cntr_pric: String? = null, // 예상체결가
    var exp_cntr_qty: String? = null, // 예상체결수량
    var _250hgst_pric_dt: String? = null, // 250최고가일
    var _250hgst_pric_pre_rt: String? = null, // 250최고가대비율
    var _250lwst_pric_dt: String? = null, // 250최저가일
    var _250lwst_pric_pre_rtm: String? = null, // 250최저가대비율
    var cur_prc: String? = null, // 현재가
    var pre_sig: String? = null, // 대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락율
    var trde_qty: String? = null, // 거래량
    var trde_pre: String? = null, // 거래대비
    var fav_unit: String? = null, // 액면가단위
    var dstr_stk: String? = null, // 유통주식
    var dstr_r: String? = null // 유통비율
)