package com.example.investfeed.common.util

import mu.KotlinLogging
import org.springframework.web.reactive.function.client.ClientResponse

private val log = KotlinLogging.logger {}

/**
 * HTTP 오류 1건을 error 로 남긴다. 모든 외부 API 클라이언트의 onStatus 에서 쓴다.
 * [asWarn]: 호출부가 실패를 모아 error 1건으로 따로 남기는 경우(여러 건 동시 조회 등)에만 true
 * [secrets]: 요청 주소에 들어간 API 키·토큰. 로그에서 *** 로 가린다 (ECOS 경로, FRED·공공데이터 쿼리 등)
 */
fun ClientResponse.logHttpError(provider: String, asWarn: Boolean = false, secrets: List<String> = emptyList()) {
    val req = request()
    val apiId = req.headers.getFirst("api-id")
    val uri = secrets.filter { it.isNotBlank() }.fold(req.uri.toString()) { acc, secret -> acc.replace(secret, "***") }

    val message = "$provider API HTTP 오류: status=${statusCode()}, ${req.method} $uri" +
        (apiId?.let { ", api-id=$it" } ?: "")
    if (asWarn) log.warn { message } else log.error { message }
}
