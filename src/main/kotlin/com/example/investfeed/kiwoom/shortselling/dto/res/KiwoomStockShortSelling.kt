package com.example.investfeed.kiwoom.shortselling.dto.res

data class KiwoomStockShortSelling(
    var dt: String? = null, // 일자
    var close_pric: String? = null, // 종가
    var pred_pre_sig: String? = null, // 전일대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락율
    var trde_qty: String? = null, // 거래량
    var shrts_qty: String? = null, // 공매도량
    var ovr_shrts_qty: String? = null, // 누적공매도량 설정 기간의 공매도량 합산데이터
    var trde_wght: String? = null, // 매매비중
    var shrts_trde_prica: String? = null, // 공매도거래대금
    var shrts_avg_pric: String? = null, // 공매도평균가
)