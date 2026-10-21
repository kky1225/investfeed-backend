package com.example.investfeed.internal.assistant.tool

import java.time.LocalDate

sealed interface AssistantDate {
    data object Today : AssistantDate
    data object Yesterday : AssistantDate
    data class On(val date: LocalDate) : AssistantDate

    fun resolve(market: StockMarket, today: LocalDate = LocalDate.now()): LocalDate? = when (this) {
        Today -> if (market == StockMarket.US) null else today
        Yesterday -> today.minusDays(1)
        is On -> date
    }

    companion object {
        fun parse(raw: String?): AssistantDate? = when (val v = raw?.trim()?.uppercase()) {
            null, "" -> null
            "TODAY" -> Today
            "YESTERDAY" -> Yesterday
            else -> runCatching { On(LocalDate.parse(v)) }.getOrElse { throw ToolException("날짜 형식이 올바르지 않습니다: $raw") }
        }
    }
}
