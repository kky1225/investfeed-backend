package com.example.investfeed.domain.index.scheduler

import com.example.investfeed.domain.index.repository.IndexDailyCloseRepository
import com.example.investfeed.domain.index.service.IndexService
import com.example.investfeed.global.holiday.HolidayService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 지수 일봉(KOSPI/KOSDAQ open/close) 수집 스케줄러 진입점.
 *
 * 22:10 시점은 키움이 일봉 정산을 늦게 반영 → open/close 둘 다 부정확.
 * 자정 직후(00:10) 로 옮겨 어제 일자 데이터가 정산 완료된 상태로 수집되게 한다.
 * (검증 단계: 자정에도 부정확하면 06~07시로 추가 이전 검토)
 *
 * RecommendScheduler 와 동일한 분리 패턴 — @Scheduled 진입은 컴포넌트가 담당,
 * 실제 수집 로직은 [HoldingGradeService.runCollectIndexClose] 외부 빈 호출.
 *
 * 진입 가드:
 *  - 어제(=수집 대상 일자) 가 휴장일이면 skip — 어제 일봉 자체가 존재하지 않으므로 호출 불필요.
 *  - 어제 일자의 KOSPI/KOSDAQ row 가 모두 이미 있으면 skip — 재시작/수동 트리거 중복 호출 방지.
 */
@Component
class IndexDailyCloseScheduler(
    private val indexService: IndexService,
    private val holidayService: HolidayService,
    private val indexDailyCloseRepository: IndexDailyCloseRepository,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val KOSPI_CD = "001"
        private const val KOSDAQ_CD = "101"
        private val YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @Scheduled(cron = "0 10 0 * * *", scheduler = "slowScheduler")
    fun scheduledCollectIndexClose() {
        log.info { "IndexDailyCloseScheduler cron fired" }
        val yesterday = LocalDate.now().minusDays(1)
        if (holidayService.isHoliday(yesterday)) {
            log.info { "IndexDailyCloseScheduler skipped: yesterday($yesterday) is holiday" }
            return
        }
        val yyyymmdd = yesterday.format(YYYYMMDD)
        val kospiExists = indexDailyCloseRepository.existsByIndsCdAndDt(KOSPI_CD, yyyymmdd)
        val kosdaqExists = indexDailyCloseRepository.existsByIndsCdAndDt(KOSDAQ_CD, yyyymmdd)
        if (kospiExists && kosdaqExists) {
            log.info { "IndexDailyCloseScheduler skipped: yesterday($yesterday) data already collected" }
            return
        }
        indexService.runCollectIndexClose()
    }
}
