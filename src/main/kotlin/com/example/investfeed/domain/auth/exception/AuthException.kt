package com.example.investfeed.domain.auth.exception

open class AuthException(
    val code: String,
    override val message: String
) : RuntimeException(message)

// 인증/인가
class UnauthorizedException : AuthException(code = "AUTH_4001", message = "인증이 필요합니다.")
class AccessDeniedException : AuthException(code = "AUTH_4003", message = "접근 권한이 없습니다.")

// 회원
class MemberNotFoundException : AuthException("AUTH_4010", "회원 정보를 찾을 수 없습니다.")

// 로그인
class InvalidCredentialsException : AuthException(code = "AUTH_4011", message = "아이디 또는 비밀번호가 올바르지 않습니다.")
class AccountLockedByFailureException : AuthException(code = "AUTH_4012", message = "로그인 실패 횟수 초과로 계정이 잠금되었습니다.")
class AccountLockedException(message: String = "계정이 잠금된 상태입니다.") : AuthException(code = "AUTH_4013", message = message)
class AccountPermanentlyLockedException : AuthException(code = "AUTH_4014", message = "계정이 영구 잠금되었습니다. 관리자에게 문의하세요.")

// 비밀번호
class InvalidPasswordException : AuthException(code = "AUTH_4015", message = "현재 비밀번호가 올바르지 않습니다.")
class SamePasswordException : AuthException(code = "AUTH_4016", message = "현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.")

// 토큰
class RefreshTokenMissingException : AuthException(code = "AUTH_4017", message = "리프레시 토큰이 없습니다.")
class RefreshTokenInvalidException : AuthException(code = "AUTH_4018", message = "유효하지 않은 리프레시 토큰입니다.")

// API Key
class ApiKeyNotFoundException : AuthException(code = "AUTH_4020", message = "API Key를 찾을 수 없습니다.")
class DuplicateApiKeyException : AuthException(code = "AUTH_4021", message = "이미 등록된 제공자의 API Key입니다.")

// 중복
class DuplicateLoginIdException : AuthException(code = "AUTH_4090", message = "이미 사용 중인 아이디입니다.")
class DuplicateEmailException : AuthException(code = "AUTH_4091", message = "이미 사용 중인 이메일입니다.")
class DuplicateNicknameException : AuthException(code = "AUTH_4092", message = "이미 사용 중인 닉네임입니다.")
class DuplicatePhoneException : AuthException(code = "AUTH_4093", message = "이미 사용 중인 전화번호입니다.")
