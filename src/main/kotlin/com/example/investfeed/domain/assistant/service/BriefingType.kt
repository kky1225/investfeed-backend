package com.example.investfeed.domain.assistant.service

enum class BriefingType(val subtype: String) {
    US_CLOSE("US_CLOSE"), KR_PRE("KR_PRE"), KR_CLOSE("KR_CLOSE"),
    KR_HOLDINGS("KR_ACCOUNT"),
    COIN_DAILY("COIN_DAILY"),
}
