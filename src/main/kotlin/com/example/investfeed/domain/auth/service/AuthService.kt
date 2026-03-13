package com.example.investfeed.domain.auth.service

import com.example.investfeed.domain.auth.dto.req.LoginReq
import com.example.investfeed.domain.auth.dto.req.ReissueReq
import com.example.investfeed.domain.auth.dto.req.SignupReq
import com.example.investfeed.domain.auth.dto.res.TokenRes
import com.example.investfeed.domain.auth.entity.Member
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.kiwoom.config.security.JwtProvider
import com.example.investfeed.kiwoom.exception.AuthException
import mu.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun signup(req: SignupReq) {
        if (memberRepository.existsByEmail(req.email)) {
            throw AuthException("AUTH_4091", "이미 사용 중인 이메일입니다.")
        }
        if (memberRepository.existsByNickname(req.nickname)) {
            throw AuthException("AUTH_4092", "이미 사용 중인 닉네임입니다.")
        }
        if (memberRepository.existsByPhone(req.phone)) {
            throw AuthException("AUTH_4093", "이미 사용 중인 전화번호입니다.")
        }

        val member = Member(
            email = req.email,
            password = passwordEncoder.encode(req.password),
            nickname = req.nickname,
            name = req.name,
            phone = req.phone
        )
        memberRepository.save(member)
        log.info { "signup: ${req.email}" }
    }

    @Transactional(readOnly = true)
    fun login(req: LoginReq): TokenRes {
        val member = memberRepository.findByEmail(req.email)
            .orElseThrow { AuthException("AUTH_4011", "이메일 또는 비밀번호가 올바르지 않습니다.") }

        if (!passwordEncoder.matches(req.password, member.password)) {
            throw AuthException("AUTH_4011", "이메일 또는 비밀번호가 올바르지 않습니다.")
        }

        log.info { "login: ${req.email}" }
        return TokenRes(
            accessToken = jwtProvider.generateAccessToken(member.email),
            refreshToken = jwtProvider.generateRefreshToken(member.email)
        )
    }

    fun reissue(req: ReissueReq): TokenRes {
        if (!jwtProvider.validateRefreshToken(req.refreshToken)) {
            throw AuthException("AUTH_4012", "유효하지 않은 리프레시 토큰입니다.")
        }

        val email = jwtProvider.getEmail(req.refreshToken)
        jwtProvider.deleteRefreshToken(email)

        return TokenRes(
            accessToken = jwtProvider.generateAccessToken(email),
            refreshToken = jwtProvider.generateRefreshToken(email)
        )
    }

    fun logout(email: String) {
        jwtProvider.deleteRefreshToken(email)
        log.info { "logout: $email" }
    }
}
