package com.example.investfeed.kiwoom.socket.dto

data class StreamEntry(
    val market: StreamMarket,
    val items: List<Any>,
    val types: List<String>,
)

enum class StreamMarket {
    KRX,
    NXT,
    US,
}
