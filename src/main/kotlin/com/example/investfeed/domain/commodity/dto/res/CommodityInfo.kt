package com.example.investfeed.domain.commodity.dto.res

data class CommodityInfo(
    var stkCd: String? = null, // 종목코드
    var stkNm: String? = null, // 종목명
    var curPrc: String? = null, // 현재가
    var predPreSig: String? = null, // 전일대비기호
    var predPre: String? = null, // 전일대비
    var fluRt: String? = null, // 등락률
    var trdeQty: String? = null, // 거래량
    var trdePrica: String? = null, // 거래대금
    var highPric: String? = null, // 고가
    var openPric: String? = null, // 시가
    var lowPric: String? = null, // 저가
    var _250hgst: String? = null, // 250최고
    var _250lwst: String? = null, // 250최저
    var tm: String? = null, // 시간
    var indNetprps: String? = null, // 개인순매수
    var frgnrNetprps: String? = null, // 외국인순매수
    var orgnNetprps: String? = null, // 기관계순매수
    var nxtEnable: String? = null, // NXT가능여부
    var orderWarning: String? = null, // 투자유의종목여부
    var marketCode: String? = null, // 시장구분코드
    var marketName: String? = null, // 시장명
)