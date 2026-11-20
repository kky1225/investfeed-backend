package com.example.investfeed.domain.auth.repository

import com.example.investfeed.domain.auth.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByLoginId(loginId: String): Optional<Member>
    fun existsByLoginId(loginId: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean
    fun existsByPhone(phone: String): Boolean
    fun existsByEmailAndLoginIdNot(email: String, loginId: String): Boolean
    fun existsByPhoneAndLoginIdNot(phone: String, loginId: String): Boolean
    fun existsByRoleId(roleId: Long): Boolean

    /**
     * 2차 비밀번호 시도 한 번을 센다. 한도(max)에 닿은 회원은 세지 않는다. 센 행의 수(0 또는 1)를 돌려준다.
     * 읽고 나서 쓰지 않고 UPDATE 한 번으로 세므로, 요청이 동시에 들어와도 한도를 넘겨 세지 않는다.
     */
    // 이 칸은 엔티티에서 읽기 전용(updatable = false)이라 SQL 로 직접 쓴다.
    @Modifying
    @Query(
        value = "UPDATE members SET failed_secondary_attempts = failed_secondary_attempts + 1 " +
            "WHERE login_id = :loginId AND failed_secondary_attempts < :max",
        nativeQuery = true,
    )
    fun claimSecondaryAttempt(loginId: String, max: Int): Int

    @Query(value = "SELECT failed_secondary_attempts FROM members WHERE login_id = :loginId", nativeQuery = true)
    fun findFailedSecondaryAttempts(loginId: String): Int?

    @Modifying
    @Query(value = "UPDATE members SET failed_secondary_attempts = 0 WHERE login_id = :loginId", nativeQuery = true)
    fun resetSecondaryAttempts(loginId: String): Int

    @Query("SELECT m.id FROM Member m WHERE m.role.code = :roleCode")
    fun findIdsByRoleCode(roleCode: String): List<Long>
}
