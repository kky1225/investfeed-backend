package com.example.investfeed.common.util

import mu.KotlinLogging
import org.springframework.web.reactive.function.client.ClientResponse

private val log = KotlinLogging.logger {}

/**
 * HTTP 오류 1건을 error 로 남긴다.
 * [asWarn]: 호출부가 실패를 모아 error 1건으로 따로 남기는 경우(여러 건 동시 조회 등)에만 true
 */
fun ClientResponse.logHttpError(provider: String, asWarn: Boolean = false) {
    val req = request()
    val apiId = req.headers.getFirst("api-id")

    val message = "$provider API HTTP 오류: status=${statusCode()}, ${req.method} ${req.uri}" +
        (apiId?.let { ", api-id=$it" } ?: "")
    if (asWarn) log.warn { message } else log.error { message }
}
