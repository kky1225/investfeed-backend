package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.entity.ApiCallDailyStat
import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.repository.ApiCallDailyStatRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ApiCallStatRollupService(
    private val apiCallCounterService: ApiCallCounterService,
    private val apiCallDailyStatRepository: ApiCallDailyStatRepository,
) {
    companion object {
        private const val ROLLUP_DAYS = 7L

        internal fun rollupDates(today: LocalDate): List<LocalDate> =
            (ROLLUP_DAYS downTo 1).map { today.minusDays(it) }

        /** 지난 날짜의 호출 수는 줄지 않는다. 줄었다면 Redis 가 초기화된 것이므로 DB 값을 지킨다 */
        internal fun canOverwrite(existingCallCount: Long, snapshot: ApiCallDailySnapshot): Boolean =
            snapshot.callCount >= existingCallCount
    }

    @Transactional
    fun rollup(today: LocalDate = LocalDate.now()): Int {
        val dates = rollupDates(today)
        val existing = apiCallDailyStatRepository.findAllByStatDateIn(dates)
            .associateBy { it.statDate to it.provider }

        val snapshots = ApiProvider.entries
            .flatMap { apiCallCounterService.getDailySnapshots(it, dates) }
            .filterNot { it.isEmpty }

        val written = snapshots.mapNotNull { snapshot ->
            val row = existing[snapshot.date to snapshot.provider.name]
            when {
                row == null -> ApiCallDailyStat.from(snapshot)
                canOverwrite(row.callCount, snapshot) -> row.also { it.overwrite(snapshot) }
                else -> null
            }
        }
        apiCallDailyStatRepository.saveAll(written)
        return written.size
    }
}
