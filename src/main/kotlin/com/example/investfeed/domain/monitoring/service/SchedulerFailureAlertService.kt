package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.notification.service.NotificationService
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * SLOW 스케줄러 실패를 슈퍼관리자·관리자의 웹 알림으로 보낸다.
 * FAST 는 매분·매초 돌아 장애 때 알림이 쏟아지므로 제외한다.
 */
@Service
class SchedulerFailureAlertService(
    private val notificationService: NotificationService,
    private val memberRepository: MemberRepository,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val RECIPIENT_ROLES = listOf("SUPER_ADMIN", "ADMIN")
        private const val TITLE_MAX = 100   // notification.asset_name 컬럼 길이

        internal fun title(schedulerName: String, reason: String?): String =
            listOfNotNull("$schedulerName 실패", reason?.takeIf { it.isNotBlank() })
                .joinToString(": ")
                .replace(Regex("\\s+"), " ")
                .take(TITLE_MAX)
    }

    /** 스케줄러의 트랜잭션이 이미 깨졌어도 저장되도록 별도 트랜잭션으로 실행한다 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun notifyFailure(scheduler: SchedulerName, reason: String?) {
        if (scheduler.type != SchedulerType.SLOW) return

        val memberIds = RECIPIENT_ROLES.flatMap { memberRepository.findIdsByRoleCode(it) }
        if (memberIds.isEmpty()) {
            log.warn { "스케줄러 실패 알림 수신 계정 없음: roles=$RECIPIENT_ROLES" }
            return
        }

        val title = title(scheduler.name, reason)
        memberIds.forEach { notificationService.createSchedulerFailureAlert(it, scheduler.name, title) }
    }
}
