package com.example.investfeed.kiwoom.sect.dto.res

import com.fasterxml.jackson.annotation.JsonProperty

data class SectPriceNowRes (
    var return_code: Int, // 결과 코드
    var return_msg: String, // 결과 메세지
    var cur_prc: String? = null, // 현재가
    var pred_pre_sig: String? = null, // 전일대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락률
    var trde_qty: String? = null, // 거래량
    var trde_prica: String? = null, // 거래대금
    var trde_frmatn_stk_num: String? = null, // 거래형성종목수
    var trde_frmatn_rt: String? = null, // 거래형성비율
    var open_pric: String? = null, // 시가
    var high_pric: String? = null, // 고가
    var low_pric: String? = null, // 저가
    var upl: String? = null, // 상한
    var rising: String? = null, // 상승
    var stdns: String? = null, // 보합
    var fall: String? = null, // 하락
    var lst: String? = null, // 하한
    @JsonProperty("52wk_hgst_pric")
    var _52wk_hgst_pric: String? = null, // 52주최고가
    @JsonProperty("52wk_hgst_pric_dt")
    var _52wk_hgst_pric_dt: String? = null, // 52주최고가일
    @JsonProperty("52wk_hgst_pric_pre_rt")
    var _52wk_hgst_pric_pre_rt: String? = null, // 52주최고가대비율
    @JsonProperty("52wk_lwst_pric")
    var _52wk_lwst_pric: String? = null, // 52주최저가
    @JsonProperty("52wk_lwst_pric_dt")
    var _52wk_lwst_pric_dt: String? = null, // 52주최저가일
    @JsonProperty("52wk_lwst_pric_pre_rt")
    var _52wk_lwst_pric_pre_rt: String? = null, // 52주최저가대비율
    var inds_cur_prc_tm: List<SectPriceNowTime>? = null // 업종현재가_시간별
)