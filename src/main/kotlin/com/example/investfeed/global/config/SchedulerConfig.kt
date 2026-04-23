package com.example.investfeed.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

/**
 * 스케줄러 스레드풀을 두 개로 분리한다.
 *
 * - fastScheduler: 분/초 단위 고빈도 스케줄러 (PriceAlert, MarketIndex)
 * - slowScheduler: 시간/일 단위 저빈도 스케줄러 (Goal, Rebalancing, Recommend 등)
 *
 * 단일 풀을 쓸 때 분단위 태스크가 외부 API 블로킹 등으로 스레드를 점유하면
 * 시간단위 알림(Goal/Rebalancing)이 수십 분 지연되는 현상이 있었음.
 * 풀을 나눠 한쪽 정체가 다른 쪽에 전이되지 않도록 격리한다.
 *
 * @Scheduled 에서 `scheduler = "fastScheduler"` 또는 `"slowScheduler"` 로 지정한다.
 * 미지정 시 @Primary 인 slowScheduler 가 사용된다 (안전한 기본값).
 */
@Configuration
class SchedulerConfig {

    @Bean("fastScheduler")
    fun fastScheduler(): ThreadPoolTaskScheduler {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = 6
        scheduler.setThreadNamePrefix("fast-scheduler-")
        scheduler.setWaitForTasksToCompleteOnShutdown(true)
        scheduler.setAwaitTerminationSeconds(30)
        scheduler.initialize()
        return scheduler
    }

    @Bean("slowScheduler")
    @Primary
    fun slowScheduler(): ThreadPoolTaskScheduler {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = 8
        scheduler.setThreadNamePrefix("slow-scheduler-")
        scheduler.setWaitForTasksToCompleteOnShutdown(true)
        scheduler.setAwaitTerminationSeconds(30)
        scheduler.initialize()
        return scheduler
    }
}
