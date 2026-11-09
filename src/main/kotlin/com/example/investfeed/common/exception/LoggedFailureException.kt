package com.example.investfeed.common.exception

/**
 * 건별 실패를 각자 기록(failure·error)한 뒤, 실행 전체를 실패로 끝내려고 모아서 던질 때 쓴다.
 * 건별 기록이 끝났으므로 받은 곳(스케줄러 등)은 warn 으로만 남긴다 (common/util/ErrorLogging.kt)
 */
class LoggedFailureException(message: String) : InvestFeedException(code = "FAILURE_9999", message = message), AlreadyLogged
