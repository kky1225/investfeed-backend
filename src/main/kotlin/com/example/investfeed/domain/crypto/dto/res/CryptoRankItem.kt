package com.example.investfeed.domain.crypto.dto.res

data class CryptoRankItem(
    val market: String,              // 마켓코드 (KRW-BTC)
    val koreanName: String,          // 한글명
    val englishName: String,         // 영문명
    val tradePrice: Double,          // 현재가
    val signedChangePrice: Double,   // 변동금액
    val signedChangeRate: Double,    // 등락률
    val change: String,              // 변동 (RISE/FALL/EVEN)
    val accTradePrice24h: Double,    // 24시간 거래대금
    val accTradeVolume24h: Double,   // 24시간 거래량
    val highPrice: Double,           // 고가
    val lowPrice: Double,            // 저가
    val prevClosingPrice: Double,    // 전일종가
    val warning: Boolean,            // 유의종목 여부
)
