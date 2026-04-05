package com.example.investfeed.common.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object DateUtil {
    fun today(pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return formatter.format(LocalDate.now())
    }

    fun time(pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return formatter.format(LocalTime.now())
    }
}
