package com.example.investfeed.common.util

import com.example.investfeed.common.exception.AlreadyLogged
import mu.KLogger
import org.springframework.web.reactive.function.client.WebClientRequestException

/*
 * 실패 1건에 error 로그 1건 원칙.
 * - 처음 알아챈 곳이 error 1건을 남긴다: 연결 실패는 ApiCallMetricsFilter, HTTP 오류는 logHttpError,
 *   return_code·응답 해석 실패는 호출 클래스.
 * - 그 뒤로는 AlreadyLogged 표시가 있는 예외(ExternalApiException 계열)로 넘기고, 받은 곳은 warn 으로만 남긴다 (error_log 에 저장되지 않음).
 * - DB 오류·NPE·기록 없이 던진 InvestFeedException 등 아직 아무도 기록하지 않은 예외는 받은 곳에서 error 로 남긴다.
 */

/** 이미 기록된 실패인지. AlreadyLogged 표시가 원인 사슬 어디에든 있으면 기록된 것으로 본다 */
fun Throwable.isAlreadyLogged(): Boolean =
    generateSequence(this) { it.cause }.take(10).any { it is AlreadyLogged }

/** 넘겨받은 예외를 기록한다. 이미 기록된 실패면 warn, 처음 보는 예외면 error(스택 포함) */
fun KLogger.failure(e: Throwable, msg: () -> String) {
    if (e.isAlreadyLogged()) {
        warn { "${msg()}: ${e.javaClass.simpleName}: ${e.message}" }
    } else {
        error(e) { msg() }
    }
}

/**
 * 외부 API 호출 클래스의 마지막 catch 에서 쓴다.
 * 연결 실패(WebClientRequestException)는 ApiCallMetricsFilter 가 이미 error 1건을 남겼으므로 warn,
 * 이미 기록된 실패도 warn, 그 밖(응답 해석 실패 등)은 여기서 error 1건.
 * 필터가 없는 WebClient·HttpURLConnection 에는 쓰지 말 것 (연결 실패가 0건이 된다).
 *
 * secretInUrl: URL 에 API 키가 들어가는 클라이언트(ECOS·FRED·공공데이터). WebClient 예외의 스택(checkpoint)에
 * 요청 URL 이 붙으므로 메시지·스택 없이 예외 종류만 남긴다. 이때 던지는 예외에도 cause 를 넣지 말 것
 */
fun KLogger.apiFailure(e: Throwable, secretInUrl: Boolean = false, msg: () -> String) {
    val alreadyLogged = e is WebClientRequestException || e.isAlreadyLogged()
    when {
        secretInUrl -> "${msg()}: ${e.javaClass.simpleName}".let { if (alreadyLogged) warn { it } else error { it } }
        alreadyLogged -> warn { "${msg()}: ${e.javaClass.simpleName}: ${e.message}" }
        else -> error(e) { msg() }
    }
}
