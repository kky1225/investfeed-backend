package com.example.investfeed.domain.auth.dto.req

import jakarta.validation.constraints.Pattern

data class SecondaryPasswordSetupReq(
    // 화면의 숫자판은 6자리만 보내지만, 요청을 직접 보내 더 짧은 값(한 자리, 빈 값)으로 만들지 못하게 서버도 확인한다.
    // 자릿수가 줄면 "10번 틀리면 정지"로 막는 효과가 없어진다.
    @field:Pattern(regexp = SECONDARY_PASSWORD_PATTERN, message = SECONDARY_PASSWORD_PATTERN_MESSAGE)
    val password: String
)

/** 2차 비밀번호의 형식: 숫자 6자리. 화면의 숫자판(SecurityKeypad)과 같은 규칙이다 */
const val SECONDARY_PASSWORD_PATTERN = "^[0-9]{6}$"
const val SECONDARY_PASSWORD_PATTERN_MESSAGE = "2차 비밀번호는 숫자 6자리로 입력해주세요."
