package com.example.investfeed.kiwoom.rank.dto.res

data class KiwoomStockTradeVolumeRes(
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var cur_prc: String? = null, // 현재가
    var pred_pre_sig: String? = null, // 전일대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락률
    var trde_qty: String? = null, // 거래량
    var pred_rt: String? = null, // 전일비
    var trde_tern_rt: String? = null, // 거래회전율
    var trde_amt: String? = null, // 거래금액
    var opmr_trde_qty: String? = null, // 장중거래량
    var opmr_pred_rt: String? = null, // 장중전일비
    var opmr_trde_rt: String? = null, // 장중거래회전율
    var opmr_trde_amt: String? = null, // 장중거래금액
    var af_mkrt_trde_qty: String? = null, // 장후거래량
    var af_mkrt_pred_rt: String? = null, // 장후전일비
    var af_mkrt_trde_rt: String? = null, // 장후거래회전율
    var af_mkrt_trde_amt: String? = null, // 장후거래금액
    var bf_mkrt_trde_qty: String? = null, // 장전거래량
    var bf_mkrt_pred_rt: String? = null, // 장전전일비
    var bf_mkrt_trde_rt: String? = null, // 장전거래회전율
    var bf_mkrt_trde_amt: String? = null // 장전거래금액
)