package com.example.investfeed.kiwoom.us.stock.dto.res

import com.example.investfeed.kiwoom.KiwoomRes
import com.fasterxml.jackson.annotation.JsonProperty

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
    @param:JsonProperty("52wk_hgst_pric")
    var wk52_hgst_pric: String? = null, // 52주 최고가
    @param:JsonProperty("52wk_hgst_pric_dt")
    var wk52_hgst_pric_dt: String? = null, // 52주 최고가 일자
    @param:JsonProperty("52wk_hgst_pric_pre_rt")
    var wk52_hgst_pric_pre_rt: String? = null, // 52주 최고가 대비율
    @param:JsonProperty("52wk_lwst_pric")
    var wk52_lwst_pric: String? = null, // 52주 최저가
    @param:JsonProperty("52wk_lwst_pric_dt")
    var wk52_lwst_pric_dt: String? = null, // 52주 최저가 일자
    @param:JsonProperty("52wk_lwst_pric_pre_rt")
    var wk52_lwst_pric_pre_rt: String? = null, // 52주 최저가 대비율
    var oyr_hgst: String? = null, // 연중 최고가
    var oyr_hgst_dt: String? = null, // 연중 최고가 일자
    var oyr_hgst_pre_rt: String? = null, // 연중 최고가 대비율
    var oyr_lwst: String? = null, // 연중 최저가
    var oyr_lwst_dt: String? = null, // 연중 최저가 일자
    var oyr_lwst_pre_rt: String? = null, // 연중 최저가 대비율
    var pre_open_pric: String? = null, // 전일 시가
    var pre_high_pric: String? = null, // 전일 고가
    var pre_low_pric: String? = null, // 전일 저가
    var base_close_pric: String? = null, // 기준종가 (전일종가)
    var open_pric: String? = null, // 당일 시가
    var high_pric: String? = null, // 당일 고가
    var low_pric: String? = null, // 당일 저가
    var stk_cnt: String? = null, // 상장주식수
    var mac: String? = null, // 시가총액 (천 USD)
    var lg_inds_cd: String? = null, // 업종 대분류
    var sm_inds_cd: String? = null, // 업종 소분류
    var curr_unit: String? = null, // 통화단위
    var trd_susp_tp: String? = null, // 거래정지구분 0:정상
): KiwoomRes(return_code, return_msg)
