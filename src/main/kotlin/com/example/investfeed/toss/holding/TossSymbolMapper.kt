package com.example.investfeed.toss.holding

object TossSymbolMapper {

    fun toStkCd(symbol: String, marketCountry: String?): String {
        return if (isUs(marketCountry)) "${symbol}_US" else "${symbol}_AL"
    }

    fun isUs(marketCountry: String?): Boolean = marketCountry == "US"
}
