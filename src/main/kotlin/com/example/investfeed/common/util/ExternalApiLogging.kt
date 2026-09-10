package com.example.investfeed.common.util

import mu.KotlinLogging
import org.springframework.web.reactive.function.client.ClientResponse

private val log = KotlinLogging.logger {}

fun ClientResponse.logHttpError(provider: String) {
    val req = request()
    val apiId = req.headers.getFirst("api-id")

    log.error {
        "$provider API HTTP 오류: status=${statusCode()}, ${req.method} ${req.uri}" +
            (apiId?.let { ", api-id=$it" } ?: "")
    }
}
