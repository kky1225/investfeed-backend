package com.example.investfeed.domain.auth.service

import com.example.investfeed.domain.auth.dto.req.LoginReq
import com.example.investfeed.domain.auth.dto.req.SignupReq
import com.example.investfeed.domain.auth.dto.res.TokenRes
import com.example.investfeed.domain.auth.entity.Member
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.security.JwtProvider
import com.example.investfeed.kiwoom.exception.AuthException
import mu.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service("memberAuthService")
class AuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun signup(
        req: SignupReq
    ) {
        if (memberRepository.existsByLoginId(req.loginId)) {
            throw AuthException("AUTH_4090", "이미 사용 중인 아이디입니다.")
        }
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
            loginId = req.loginId,
            password = passwordEncoder.encode(req.password),
            email = req.email,
            nickname = req.nickname,
            name = req.name,
            phone = req.phone
        )

        memberRepository.save(member)
    }

    @Transactional(readOnly = true)
    fun login(
        req: LoginReq
    ): Pair<TokenRes, String> {
        val member = memberRepository.findByLoginId(req.loginId)
            .orElseThrow { AuthException("AUTH_4011", "아이디 또는 비밀번호가 올바르지 않습니다.") }

        if (!passwordEncoder.matches(req.password, member.password)) {
            throw AuthException("AUTH_4011", "아이디 또는 비밀번호가 올바르지 않습니다.")
        }

        log.info { "login: ${req.loginId}" }
        return Pair(
            TokenRes(accessToken = jwtProvider.generateAccessToken(member.loginId)),
            jwtProvider.generateRefreshToken(member.loginId)
        )
    }

    fun reissue(
        refreshToken: String?
    ): Pair<TokenRes, String> {
        if (refreshToken == null) {
            throw AuthException("AUTH_4012", "리프레시 토큰이 없습니다.")
        }

        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw AuthException("AUTH_4012", "유효하지 않은 리프레시 토큰입니다.")
        }

        val loginId = jwtProvider.getLoginId(refreshToken)
        jwtProvider.deleteRefreshToken(loginId)

        return Pair(
            TokenRes(accessToken = jwtProvider.generateAccessToken(loginId)),
            jwtProvider.generateRefreshToken(loginId)
        )
    }

    fun logout(
        loginId: String
    ) {
        jwtProvider.deleteRefreshToken(loginId)
    }
}
