package com.example.investfeed.kiwoom.sect.dto.req

data class SectCodeListReq(
    var mrkt_tp: String // 시장구분 0:코스피(거래소),1:코스닥,2:KOSPI200,4:KOSPI100,7:KRX100(통합지수)
)