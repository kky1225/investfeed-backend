package com.example.investfeed.domain.auth.exception

import com.example.investfeed.common.exception.InvestFeedException
import org.springframework.security.core.AuthenticationException

// 회원
class MemberNotFoundException : InvestFeedException("AUTH_4010", "회원 정보를 찾을 수 없습니다.")

// 로그인 (401)
class InvalidCredentialsException : AuthenticationException("아이디 또는 비밀번호가 올바르지 않습니다.")
class AccountLockedByFailureException : AuthenticationException("로그인 실패 횟수 초과로 계정이 잠금되었습니다.")
class AccountLockedException(message: String = "계정이 잠금된 상태입니다.") : AuthenticationException(message)
class AccountPermanentlyLockedException : AuthenticationException("계정이 영구 잠금되었습니다. 관리자에게 문의하세요.")

// 비밀번호
class InvalidPasswordException : InvestFeedException(code = "AUTH_4015", message = "현재 비밀번호가 올바르지 않습니다.")
class SamePasswordException : InvestFeedException(code = "AUTH_4016", message = "현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.")

// 토큰 (401)
class RefreshTokenMissingException : AuthenticationException("리프레시 토큰이 없습니다.")
class RefreshTokenInvalidException : AuthenticationException("유효하지 않은 리프레시 토큰입니다.")

// API Key
class ApiKeyNotFoundException : InvestFeedException(code = "AUTH_4020", message = "API Key를 찾을 수 없습니다.")
class DuplicateApiKeyException : InvestFeedException(code = "AUTH_4021", message = "이미 등록된 제공자의 API Key입니다.")

// 중복
class DuplicateLoginIdException : InvestFeedException(code = "AUTH_4090", message = "이미 사용 중인 아이디입니다.")
class DuplicateEmailException : InvestFeedException(code = "AUTH_4091", message = "이미 사용 중인 이메일입니다.")
class DuplicateNicknameException : InvestFeedException(code = "AUTH_4092", message = "이미 사용 중인 닉네임입니다.")
class DuplicatePhoneException : InvestFeedException(code = "AUTH_4093", message = "이미 사용 중인 전화번호입니다.")
