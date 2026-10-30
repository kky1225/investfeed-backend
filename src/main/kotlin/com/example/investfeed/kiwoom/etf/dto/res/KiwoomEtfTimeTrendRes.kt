package com.example.investfeed.kiwoom.etf.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomEtfTimeTrendRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var stk_nm: String? = null, // 종목명
    var etfobjt_idex_nm: String? = null, // ETF대상지수명
    var wonju_pric: String? = null, // 원주가격
    var etftxon_type: String? = null, // ETF과세유형
    var etntxon_type: String? = null, // ETN과세유형
    var etftisl_trnsn: List<KiwoomEtfTimeTrendItem>? = null, // ETF시간대별추이 (최근 시각부터, 장마감 후 첫 행 tm=888888)
): KiwoomRes(return_code, return_msg)

data class KiwoomEtfTimeTrendItem(
    var tm: String? = null, // 시간 HHmmss
    var close_pric: String? = null, // 종가
    var pre_sig: String? = null, // 대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락율 (%)
    var trde_qty: String? = null, // 거래량
    var nav: String? = null, // NAV
    var trde_prica: String? = null, // 거래대금 (백만원)
    var navidex: String? = null, // NAV지수 괴리 (NAV vs 추적지수, %)
    var navetf: String? = null, // 괴리율 (시장가 vs NAV, %)
    var trace: String? = null, // 추적오차율 (%). ka40003 trace_eor_rt 와 단위가 100배 다르다
    var trace_idex: String? = null, // 추적지수
    var trace_idex_pred_pre: String? = null, // 추적지수 전일대비
    var trace_idex_pred_pre_sig: String? = null, // 추적지수 대비기호
)
