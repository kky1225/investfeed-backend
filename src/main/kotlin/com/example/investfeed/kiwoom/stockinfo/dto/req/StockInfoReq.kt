package com.example.investfeed.kiwoom.stockinfo.dto.req

data class StockInfoReq (
    var mrkt_tp: String // 시장구분 0:코스피,10:코스닥,3:ELW,8:ETF,30:K-OTC,50:코넥스,5:신주인수권,4:뮤추얼펀드,6:리츠,9:하이일드
)