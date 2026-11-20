package com.example.investfeed.domain.calendar.dto.res

data class MarketSessionRes(
    val id: Long,
    val date: String,       // yyyy-MM-dd
    val name: String,
    val openTime: String,   // HH:mm
    val closeTime: String,  // HH:mm
    val preMarket: Boolean,
)
