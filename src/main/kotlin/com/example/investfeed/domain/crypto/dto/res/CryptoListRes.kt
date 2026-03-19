package com.example.investfeed.domain.crypto.dto.res

data class CryptoListRes(
    var cryptoList: List<CryptoListItem>,
    var fearGreed: FearGreedRes,
)
