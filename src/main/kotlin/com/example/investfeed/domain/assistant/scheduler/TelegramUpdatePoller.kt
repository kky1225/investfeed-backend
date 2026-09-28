package com.example.investfeed.domain.assistant.scheduler

import com.example.investfeed.domain.assistant.service.TelegramCommandService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.telegram.client.TelegramClient
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TelegramUpdatePoller(
    private val telegramClient: TelegramClient,
    private val telegramCommandService: TelegramCommandService,
    private val redisTemplate: RedisTemplate<String, String>,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    private val offsetKey = "${RedisKeyPrefix.ASSISTANT.prefix}TELEGRAM:OFFSET"

    private var consecutiveFailures = 0
    private var commandsRegistered = false

    @Scheduled(cron = SchedulerCron.TELEGRAM_POLL, scheduler = "fastScheduler")
    fun poll() {
        if (!telegramClient.enabled) return
        schedulerLogService.markFired(SchedulerName.TelegramUpdatePoller)
        schedulerLogService.execute(SchedulerName.TelegramUpdatePoller) { pollOnce() }   // FAST: 성공은 scheduler_status 만, timeout 초과·예외만 scheduler_log
    }

    private fun pollOnce() {
        if (!commandsRegistered) {
            commandsRegistered = telegramClient.setMyCommands(TelegramCommandService.BOT_COMMANDS)
            if (commandsRegistered) log.info { "텔레그램 봇 메뉴 등록: ${TelegramCommandService.BOT_COMMANDS.joinToString { "/" + it.command }}" }
        }
        val offset = redisTemplate.opsForValue().get(offsetKey)?.toLongOrNull()
        val updates = try {
            telegramClient.getUpdates(offset)
        } catch (e: TelegramClient.GetUpdatesException) {
            consecutiveFailures++
            if (consecutiveFailures == 1) log.error { "텔레그램 폴링 실패 시작: ${e.message}" }
            else log.warn { "텔레그램 폴링 실패 ${consecutiveFailures}회 연속: ${e.message}" }
            return
        }
        if (consecutiveFailures > 0) {
            log.error { "텔레그램 폴링 복구: ${consecutiveFailures}회 연속 실패 후 정상 (약 ${consecutiveFailures * 5}초)" }
            consecutiveFailures = 0
        }
        updates.forEach { update ->
            runCatching { telegramCommandService.handle(update) }
                .onFailure { log.error(it) { "텔레그램 업데이트 처리 실패: update_id=${update.update_id}" } }
            redisTemplate.opsForValue().set(offsetKey, (update.update_id + 1).toString())
        }
    }
}
