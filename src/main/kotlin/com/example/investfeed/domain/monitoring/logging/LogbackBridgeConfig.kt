package com.example.investfeed.domain.monitoring.logging

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import com.example.investfeed.domain.monitoring.repository.ErrorLogRepository
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

/**
 * Spring Bean (ErrorLogRepository) 을 Logback Appender 에 주입하고,
 * AsyncAppender 로 감싸서 ROOT 로거에 등록한다.
 *
 * logback.xml 에서 직접 Spring Bean 을 참조할 수 없기 때문에 런타임 브릿지로 연결.
 */
@Configuration
class LogbackBridgeConfig(
    private val errorLogRepository: ErrorLogRepository,
) {
    private val log = KotlinLogging.logger {}

    @PostConstruct
    fun registerDbErrorAppender() {
        DbErrorLogAppender.repository = errorLogRepository

        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val root = context.getLogger(Logger.ROOT_LOGGER_NAME)

        val dbAppender = DbErrorLogAppender().apply {
            this.context = context
            name = "DB_ERROR_LOG"
            start()
        }

        // Async wrapper — 에러 저장이 요청 스레드를 막지 않도록
        val asyncAppender = AsyncAppender().apply {
            this.context = context
            name = "ASYNC_DB_ERROR_LOG"
            queueSize = 500
            discardingThreshold = 0              // 큐 꽉 차면 기본적으로 INFO/WARN drop → 0 으로 전부 유지
            isIncludeCallerData = false          // 성능 위해 caller data 생략
            addAppender(dbAppender)
            start()
        }

        root.addAppender(asyncAppender)
        log.info { "DbErrorLogAppender 등록 완료 — ERROR 레벨 로그는 error_log 테이블에 자동 저장됩니다." }
    }
}
