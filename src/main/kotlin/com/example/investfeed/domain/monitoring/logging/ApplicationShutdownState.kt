package com.example.investfeed.domain.monitoring.logging

import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 애플리케이션 종료 감지용 전역 상태.
 *
 * 서버 shutdown 시작 후 발생하는 Netty/Lettuce/WebClient 관련 race condition 에러가
 * DB 에 기록되는 것을 막기 위함.
 *
 * - Spring `ContextClosedEvent` 수신 시 플래그 true 로 전환
 * - `DbErrorLogAppender`, `SchedulerLogService` 등이 플래그 확인 후 DB 저장 skip
 * - 파일 로그(`investfeed.log`) 는 그대로 기록됨 → 필요 시 역추적 가능
 */
object ApplicationShutdownState {
    // 스레드 값 동기화를 위한 어노테이션
    @Volatile
    var isShuttingDown: Boolean = false
}

@Component
class ApplicationShutdownListener {
    @EventListener(ContextClosedEvent::class)
    fun onShutdown() {
        ApplicationShutdownState.isShuttingDown = true
    }
}
