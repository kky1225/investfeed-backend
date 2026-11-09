package com.example.investfeed.common.exception

/** 기록을 마친 실패라는 표시. 이 표시가 있는 예외를 받은 곳은 error 를 다시 남기지 않는다 (common/util/ErrorLogging.kt) */
interface AlreadyLogged

/**
 * 외부 API 실패 예외의 기반 클래스. 던지는 쪽(호출 클래스·공통 필터)이 error 1건을 남긴 뒤 던진다.
 * 기록 없이 던지는 예외(토큰 없음 등)는 이 클래스를 쓰지 말고 InvestFeedException 을 쓴다.
 */
open class ExternalApiException(
    code: String,
    message: String,
    cause: Throwable? = null,
) : InvestFeedException(code, message, cause), AlreadyLogged
