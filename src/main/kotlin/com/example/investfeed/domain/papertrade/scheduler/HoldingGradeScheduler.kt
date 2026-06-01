package com.example.investfeed.domain.papertrade.scheduler

import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.papertrade.service.HoldingGradeService
import com.example.investfeed.global.holiday.HolidayService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 보유 평가 스케줄러 진입점. @Scheduled 메서드를 HoldingGradeService 와 분리해서
 * runHoldingGrade 호출이 Spring AOP 프록시를 거치도록 함 (= @Transactional 정상 적용).
 * 같은 클래스 self-invocation 이면 트랜잭션이 안 걸려서 향후 @Modifying 쿼리/lazy 로딩
 * 등 추가 시 TransactionRequiredException 으로 터질 수 있음 — RecommendScheduler 와
 * 동일 분리 패턴.
 */
@Component
class HoldingGradeScheduler(
    private val holdingGradeService: HoldingGradeService,
    private val holidayService: HolidayService,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 10 22 * * *", scheduler = "slowScheduler")
    fun scheduledHoldingGrade() {
        log.info { "HoldingGradeScheduler cron fired" }
        if (holidayService.isHoliday()) {
            log.info { "HoldingGradeScheduler skipped: today is holiday" }
            return
        }
        // 22:00 추천이 끝난 뒤 평가해야 "이미 평가된 후보 제외"가 정확. 단일 SLOW 스레드라
        // 보통 큐잉으로 자연 직렬화되지만, 수동 트리거/지연 대비 명시 가드.
        if (schedulerLogService.isRunning(SchedulerName.RecommendScheduler)) {
            log.warn { "HoldingGradeScheduler skipped: RecommendScheduler 실행 중 (추천 완료 후 평가)" }
            return
        }
        if (schedulerLogService.isRunning(SchedulerName.BacktestBackfillScheduler)) {
            log.warn { "HoldingGradeScheduler skipped: BacktestBackfillScheduler 실행 중" }
            return
        }
        holdingGradeService.runHoldingGrade()
    }
}
