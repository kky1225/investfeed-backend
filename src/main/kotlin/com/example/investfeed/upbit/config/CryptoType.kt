package com.example.investfeed.upbit.config

enum class CryptoType(
    val market: String,
    val koreanName: String,
    val englishName: String,
) {
    BITCOIN(market = "KRW-BTC", koreanName = "비트코인", englishName = "Bitcoin"),
    ETHEREUM(market = "KRW-ETH", koreanName = "이더리움", englishName = "Ethereum"),
}
