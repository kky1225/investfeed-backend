package com.example.investfeed.common.exception

/**
 * 외부 API 호출 클래스의 마지막 catch 에서 던진다 (연결 실패·응답 해석 실패 등).
 * 던지기 전에 apiFailure() 로 기록을 마친 상태이므로 위에서는 warn 으로만 남긴다.
 * message 는 화면 응답에 그대로 나가므로 고정 문구, 원인은 cause 로 보존한다.
 */
class ExternalApiFailureException(
    cause: Throwable? = null,
    message: String = "외부 API 통신 오류가 발생하였습니다.",
) : ExternalApiException(code = "API_9999", message = message, cause = cause)
