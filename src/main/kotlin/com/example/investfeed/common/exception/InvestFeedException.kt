package com.example.investfeed.common.exception

/**
 * 서비스 예외의 기반 클래스.
 * 외부 API 실패는 처음 알아챈 곳(호출 클래스·공통 필터)이 error 1건을 남긴 뒤 이 타입으로 넘긴다.
 * 그래서 이 타입을 받은 곳은 다시 error 로 남기지 않는다 (common/util/ErrorLogging.kt).
 */
open class InvestFeedException(
    val code: String,
    override val message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
