package com.example.investfeed.domain.assistant.service

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object TemplateFormat {
    fun round(v: Double, digits: Int): Double {
        val r = Math.round(v * Math.pow(10.0, digits.toDouble())) / Math.pow(10.0, digits.toDouble())
        return if (r == 0.0) 0.0 else r
    }

    fun num(v: Double, digits: Int = 0): String = String.format(Locale.KOREA, "%,.${digits}f", round(v, digits))

    fun num(v: Long): String = String.format(Locale.KOREA, "%,d", v)

    fun sign(v: Double): String = if (v > 0) "+" else if (v < 0) "-" else ""

    fun won(v: Long): String = "${num(v)}원"

    fun signedWon(v: Long): String = "${sign(v.toDouble())}${num(abs(v))}원"

    fun usd(v: Double): String = "$" + num(abs(v), 2)

    fun signedUsd(v: Double): String = "${sign(v)}${usd(v)}"

    fun rate(v: Double, digits: Int = 1): String = round(v, digits).let { r -> "${sign(r)}${num(abs(r), digits)}%" }

    fun bp(v: Double): String {
        val r = v.roundToInt()
        return "${sign(r.toDouble())}${abs(r)}bp"
    }

    fun eok(v: Long): String {
        val a = abs(v)
        val body = if (a >= 10_000) "${num(a / 10_000.0, 1)}조" else "${num(a)}억"
        return "${sign(v.toDouble())}$body"
    }

    fun colored(text: String, v: Double?): String = when {
        v == null || round(v, 2) == 0.0 -> text
        v > 0 -> "{$text|up}"
        else -> "{$text|down}"
    }

    fun coloredRate(v: Double, digits: Int = 1): String = colored(rate(v, digits), round(v, digits))
    fun coloredWon(v: Long): String = colored(signedWon(v), v.toDouble())
    fun coloredUsd(v: Double): String = colored(signedUsd(v), v)
    fun coloredEok(v: Long): String = colored(eok(v), v.toDouble())
    fun coloredBp(v: Double): String = colored(bp(v), v.roundToInt().toDouble())   // 표시가 0bp 면 색 없음

    fun parse(s: String?): Double? {
        if (s.isNullOrBlank()) return null
        val cleaned = s.trim().replace("--", "-").replace(",", "").removeSuffix("%").removePrefix("+")
        return cleaned.toDoubleOrNull()
    }

    fun parseLong(s: String?): Long? = parse(s)?.let { Math.round(it) }

    fun weekday(date: LocalDate): String = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "월"; DayOfWeek.TUESDAY -> "화"; DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"; DayOfWeek.FRIDAY -> "금"; DayOfWeek.SATURDAY -> "토"; DayOfWeek.SUNDAY -> "일"
    }

    fun monthDay(date: LocalDate): String = "${date.monthValue}/${date.dayOfMonth}(${weekday(date)})"

    fun fearGreedLabel(v: Int): String = when {
        v <= 24 -> "극단적 공포"; v <= 44 -> "공포"; v <= 55 -> "중립"; v <= 75 -> "탐욕"; else -> "극단적 탐욕"
    }

    fun streakSuffix(days: Int): String = if (days >= 3) " (${days}일째)" else ""

    fun coloredGrade(grade: String): String = when (grade) {
        "STRONG_BUY", "BUY" -> "{$grade|up}"
        "SELL", "STRONG_SELL" -> "{$grade|down}"
        else -> grade
    }
}
