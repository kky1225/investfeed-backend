package com.example.investfeed.domain.auth.exception

import com.example.investfeed.common.exception.InvestFeedException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException

// 회원
class MemberNotFoundException : InvestFeedException("AUTH_4010", "회원 정보를 찾을 수 없습니다.")

// 로그인 (401)
class InvalidCredentialsException : AuthenticationException("아이디 또는 비밀번호가 올바르지 않습니다.")
class AccountLockedByFailureException(val lockRemainingSeconds: Long) : AuthenticationException("로그인 실패 횟수 초과로 계정이 잠금되었습니다.")
class AccountLockedException(val lockRemainingSeconds: Long) : AuthenticationException("계정이 잠금된 상태입니다.")
class AccountPermanentlyLockedException : AuthenticationException("계정이 영구 잠금되었습니다. 관리자에게 문의하세요.")

// 비밀번호
class InvalidPasswordException : InvestFeedException(code = "AUTH_4015", message = "현재 비밀번호가 올바르지 않습니다.")
class SamePasswordException : InvestFeedException(code = "AUTH_4016", message = "현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.")

// 토큰 (401)
class RefreshTokenMissingException : AuthenticationException("리프레시 토큰이 없습니다.")
class RefreshTokenInvalidException : AuthenticationException("유효하지 않은 리프레시 토큰입니다.")
class RefreshTokenReuseDetectedException : AuthenticationException("리프레시 토큰 재사용이 감지되어 로그아웃되었습니다.")

// API Key
class ApiKeyNotFoundException : InvestFeedException(code = "AUTH_4020", message = "API Key를 찾을 수 없습니다.")
class DuplicateApiKeyException : InvestFeedException(code = "AUTH_4021", message = "이미 등록된 제공자의 API Key입니다.")
class InvalidApiKeyException : InvestFeedException(code = "AUTH_4022", message = "유효하지 않은 API Key 입니다.")
class ApiKeyRegistrationLockedException : InvestFeedException(code = "AUTH_4023", message = "API Key 등록이 잠겨 있습니다. 보안 정책상 5회 실패 시 모든 broker 등록이 차단됩니다. 관리자에게 문의해주세요.")

// TOTP
class TotpNotSetupException : InvestFeedException(code = "AUTH_4030", message = "TOTP가 설정되지 않았습니다. 먼저 TOTP 설정을 진행하세요.")
class InvalidTotpCodeException : AuthenticationException("TOTP 인증 코드가 올바르지 않습니다.")
class PreAuthTokenMissingException : AuthenticationException("사전 인증 토큰이 없습니다.")
class PreAuthTokenInvalidException : AuthenticationException("유효하지 않은 사전 인증 토큰입니다.")

// 2차 비밀번호
class SecondaryPasswordNotSetException : InvestFeedException(code = "AUTH_4041", message = "2차 비밀번호가 설정되지 않았습니다.")
class InvalidSecondaryPasswordException : InvestFeedException(code = "AUTH_4042", message = "2차 비밀번호가 올바르지 않습니다.")
class SecondaryPasswordLockedException(val remainingSeconds: Long) : AccessDeniedException("2차 비밀번호 입력이 잠금되었습니다. ${remainingSeconds / 60 + 1}분 후에 다시 시도하세요.")
class SameSecondaryPasswordException : InvestFeedException(code = "AUTH_4043", message = "현재 2차 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.")

// 중복
class DuplicateLoginIdException : InvestFeedException(code = "AUTH_4090", message = "이미 사용 중인 아이디입니다.")
class DuplicateEmailException : InvestFeedException(code = "AUTH_4091", message = "이미 사용 중인 이메일입니다.")
class DuplicateNicknameException : InvestFeedException(code = "AUTH_4092", message = "이미 사용 중인 닉네임입니다.")
class DuplicatePhoneException : InvestFeedException(code = "AUTH_4093", message = "이미 사용 중인 전화번호입니다.")
