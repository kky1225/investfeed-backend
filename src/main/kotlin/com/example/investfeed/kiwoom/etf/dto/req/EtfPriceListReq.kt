package com.example.investfeed.kiwoom.etf.dto.req

data class EtfPriceListReq(
    var txon_type: String, // 과세유형 0:전체, 1:비과세, 2:보유기간과세, 3:회사형, 4:외국, 5:비과세해외(보유기간관세)
    var navpre: String, // NAV 대비 0:전체, 1:NAV > 전일종가, 2:NAV < 전일종가
    var mngmcomp: String, // 운용사 0000:전체, 3020:KODEX(삼성), 3027:KOSEF(키움), 3191:TIGER(미래에셋), 3228:KINDEX(한국투자), 3023:KStar(KB), 3022:아리랑(한화), 9999:기타운용사
    var txon_yn: String, // 과세여부 0:전체, 1:과세, 2:비과세
    var trace_idex: String = "0", // 추적지수 0:전체
    var stex_tp: String // 거래소구분 1:KRX, 2:NXT, 3:통합
)