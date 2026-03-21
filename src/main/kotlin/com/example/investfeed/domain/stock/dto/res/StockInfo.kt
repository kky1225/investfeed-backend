package com.example.investfeed.domain.stock.dto.res

data class StockInfo(
    var stkCd: String? = null, // 종목코드
    var stkNm: String? = null, // 종목명
    var per: String? = null, // PER
    var eps: String? = null, // EPS
    var roe: String? = null, // ROE
    var pbr: String? = null, // PBR
    var mac: String? = null, // 시가총액
    var macWght: String? = null, // 시가총액비중
    var forExhRt: String? = null, // 외인소진률
    var _250hgst: String? = null, // 250최고
    var _250lwst: String? = null, // 250최저
    var highPric: String? = null, // 고가
    var openPric: String? = null, // 시가
    var lowPric: String? = null, // 저가
    var curPrc: String? = null, // 현재가
    var preSig: String? = null, // 대비기호
    var predPre: String? = null, // 전일대비
    var fluRt: String? = null, // 등락율
    var trdeQty: String? = null, // 거래량
    var tm: String? = null, // 시간
    var trdePrica: String? = null, // 누적거래대금
    var nxtEnable: String? = null, // NXT가능여부
    var orderWarning: String? = null, // 투자유의종목여부
    var marketCode: String? = null, // 시장구분코드
    var marketName: String? = null, // 시장명
    var upName: String? = null, // 업종명
)