package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.entity.TelegramStatus
import com.example.investfeed.domain.assistant.repository.AssistantSettingRepository
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.telegram.client.TelegramClient
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TelegramNotifier(
    private val telegramClient: TelegramClient,
    private val assistantSettingRepository: AssistantSettingRepository,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    private val sendBlockedKey = "${RedisKeyPrefix.ASSISTANT.prefix}TELEGRAM:SEND_BLOCKED"

    fun isSendBlocked(): Boolean = redisTemplate.hasKey(sendBlockedKey)

    fun setSendBlocked(blocked: Boolean) {
        if (blocked) redisTemplate.opsForValue().set(sendBlockedKey, LocalDateTime.now().toString())
        else redisTemplate.delete(sendBlockedKey)
        log.info { "텔레그램 전체 발송 ${if (blocked) "차단" else "재개"} (관리자)" }
    }

    @Transactional
    fun notify(memberId: Long, html: String): Boolean {
        if (!telegramClient.enabled || isSendBlocked()) return false
        val setting = assistantSettingRepository.findById(memberId).orElse(null) ?: return false
        val chatId = setting.telegramChatId ?: return false
        if (setting.telegramStatus != TelegramStatus.ACTIVE) return false

        return when (val r = telegramClient.sendHtml(chatId, html)) {
            TelegramClient.SendResult.Sent -> true
            TelegramClient.SendResult.Blocked -> {
                setting.telegramStatus = TelegramStatus.BLOCKED
                setting.updatedAt = LocalDateTime.now()
                assistantSettingRepository.save(setting)
                log.warn { "텔레그램 봇 차단 감지, 발송 중지: member=$memberId" }
                false
            }
            is TelegramClient.SendResult.Failed -> {
                log.error { "텔레그램 발송 실패: member=$memberId ${r.reason}" }
                false
            }
        }
    }
}
