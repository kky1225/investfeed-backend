package com.example.investfeed.kiwoom.support

import kotlinx.coroutines.delay
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

private val QUOTA_EXCEEDED_CODES = setOf(1700, 1701, 1702)

suspend fun <T> withQuotaRetry(
    apiId: String,
    returnCode: (T) -> Int?,
    backoffMs: Long = 1_000,
    block: suspend () -> T?,
): T? {
    val first = block()
    val code = first?.let(returnCode)
    if (code == null || code !in QUOTA_EXCEEDED_CODES) return first

    log.warn { "키움 한도 초과 응답(return_code=$code) — ${backoffMs}ms 후 1회 재시도: api=$apiId" }
    delay(backoffMs)
    return block()
}
