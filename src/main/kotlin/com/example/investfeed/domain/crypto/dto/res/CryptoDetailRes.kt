package com.example.investfeed.domain.crypto.dto.res

data class CryptoDetailRes(
    var cryptoInfo: CryptoDetailInfo,
    var chartList: List<CryptoChart>,
)
