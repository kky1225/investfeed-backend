package com.example.investfeed.kiwoom.socket.dto

data class KiwoomStreamReq(
    var trnm: String = "REG", // 서비스명 REG : 등록, REMOVE : 해지
    var grp_no: String = "0", // 그룹번호
    var refresh: String = "0", // 등록(REG)시 0:기존등록 해지, 1:기존등록 유지
    var data: List<KiwoomStream>? = null // 실시간 등록 리스트
)

data class KiwoomStream(
    var item: List<Any>? = null, // 국내는 종목/업종 코드 문자열(005930_AL), 미국은 [KiwoomUsStreamItem]
    var type: List<String> // 실시간 항목 TR 명 (0B, 04, 0J, FE ...)
)

data class KiwoomUsStreamItem(
    var jmcode: String, // 종목코드 (티커)
    var stex_tp: String, // 거래소구분 ND:NASDAQ, NY:NYSE, NA:AMEX
)
