package com.example.investfeed.domain.auth.service

import com.example.investfeed.domain.auth.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 2차 비밀번호를 틀린 횟수를 센다. 맞히지 못한 채 연달아 MAX_ATTEMPTS 번 틀리면 2차 비밀번호가 정지되고,
 * 관리자가 초기화(AuthService.resetSecondaryPassword)할 때까지 인증도 변경도 할 수 없다. 시간이 지나도 풀리지 않는다.
 *
 * 횟수는 부르는 쪽의 트랜잭션과 따로 저장한다(REQUIRES_NEW). 틀려서 예외로 끝나 부르는 쪽이 되돌려져도 센 횟수는 남아야 한다.
 */
@Service
class SecondaryPasswordAttemptService(
    private val memberRepository: MemberRepository
) {
    companion object {
        const val MAX_ATTEMPTS = 10

        /** 이 횟수면 2차 비밀번호가 정지된 것이다 */
        fun isSuspended(failedAttempts: Int): Boolean = failedAttempts >= MAX_ATTEMPTS
    }

    /**
     * 대조하기 전에 시도 한 번을 먼저 센다. 센 뒤의 누적 횟수를 돌려주고, 이미 한도에 닿아 세지 못했으면 null.
     * 먼저 세야 요청을 한꺼번에 여러 개 보내도 대조되는 것이 한도를 넘지 않는다. 맞혔으면 reset 으로 되돌린다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun claim(loginId: String): Int? {
        if (memberRepository.claimSecondaryAttempt(loginId, MAX_ATTEMPTS) == 0) return null
        return memberRepository.findFailedSecondaryAttempts(loginId)
    }

    /** 맞혔을 때 횟수를 0 으로 돌린다. (관리자 초기화는 자기 트랜잭션 안에서 같은 쿼리를 직접 부른다) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun reset(loginId: String) {
        memberRepository.resetSecondaryAttempts(loginId)
    }
}
