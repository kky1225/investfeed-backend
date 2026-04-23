package com.example.investfeed.domain.monitoring.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.core.AppenderBase
import com.example.investfeed.domain.monitoring.entity.ErrorLog
import com.example.investfeed.domain.monitoring.repository.ErrorLogRepository
import java.time.Instant
import java.time.ZoneId

/**
 * ERROR 레벨 로그를 `error_log` 테이블에 저장하는 Logback Appender.
 *
 * - 애플리케이션 코드에서 `log.error {}` 호출하면 자동으로 DB 에 기록됨
 * - 라이브러리 내부 ERROR 로그(Lettuce, Hibernate 등)도 자동 수집
 * - repository 는 Spring 기동 시 LogbackBridgeConfig 에서 주입
 * - 기동 전 발생한 ERROR 는 DB 저장 불가 → 파일 appender 로 백업
 *
 * 무한 루프 방지:
 * - repository 가 null 이면 조용히 무시
 * - 자기 자신이 생성한 에러는 스킵
 * - 모든 예외는 runCatching 으로 삼킴 (에러 로깅 자체가 실패해도 서비스 영향 없음)
 */
class DbErrorLogAppender : AppenderBase<ILoggingEvent>() {

    companion object {
        @Volatile
        var repository: ErrorLogRepository? = null
    }

    override fun append(event: ILoggingEvent) {
        if (event.level != Level.ERROR) return
        // 서버 shutdown 중 발생하는 Netty/Lettuce race 에러는 DB 저장 skip (파일 로그에는 남음)
        if (ApplicationShutdownState.isShuttingDown) return
        val errorLogRepository = repository ?: return

        // 자기 자신 로거 skip — 무한 루프 방지
        val loggerName = event.loggerName ?: return
        if (loggerName.contains("DbErrorLogAppender")) return

        runCatching {
            val occurredAt = Instant.ofEpochMilli(event.timeStamp)
                .atZone(ZoneId.systemDefault()).toLocalDateTime()
            // MDC 에 schedulerName 이 있으면 → 스케줄러 실행 중 발생한 에러
            val schedulerName = event.mdcPropertyMap?.get("schedulerName")?.take(100)
            errorLogRepository.save(
                ErrorLog(
                    occurredAt = occurredAt,
                    loggerName = loggerName.take(255),
                    threadName = event.threadName?.take(100),
                    schedulerName = schedulerName,
                    message = event.formattedMessage?.take(10000),
                    stackTrace = event.throwableProxy?.let { buildStackTrace(it) }?.take(20000),
                )
            )
        }
        // 실패해도 무시. STDOUT/파일 appender 가 이미 기록했으므로 손실 없음.
    }

    private fun buildStackTrace(tp: IThrowableProxy): String {
        val sb = StringBuilder()
        sb.append(tp.className).append(": ").append(tp.message ?: "").append('\n')
        tp.stackTraceElementProxyArray?.forEach {
            sb.append("\tat ").append(it.steAsString).append('\n')
        }
        tp.cause?.let {
            sb.append("Caused by: ")
            sb.append(buildStackTrace(it))
        }
        return sb.toString()
    }
}
