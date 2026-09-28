package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.res.TelegramLinkCodeRes
import com.example.investfeed.domain.assistant.dto.res.TelegramStatusRes
import com.example.investfeed.domain.assistant.entity.AssistantSetting
import com.example.investfeed.domain.assistant.entity.TelegramStatus
import com.example.investfeed.domain.assistant.repository.AssistantSettingRepository
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.telegram.client.TelegramClient
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime

@Service
class TelegramLinkService(
    private val assistantSettingRepository: AssistantSettingRepository,
    private val assistantSettingService: AssistantSettingService,
    private val telegramClient: TelegramClient,
    private val telegramNotifier: TelegramNotifier,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val CODE_TTL: Duration = Duration.ofMinutes(10)
    }

    private val random = SecureRandom()

    private fun codeKey(code: String) = "${RedisKeyPrefix.ASSISTANT.prefix}TELEGRAM:LINK:$code"

    fun status(memberId: Long): TelegramStatusRes {
        val s = assistantSettingService.findOrDefault(memberId)
        return TelegramStatusRes(
            configured = telegramClient.enabled,
            status = s.telegramStatus,
            linkedAt = s.telegramLinkedAt,
            botUsername = telegramClient.botUsername,
            sendBlocked = telegramNotifier.isSendBlocked(),
        )
    }

    fun issueLinkCode(memberId: Long): TelegramLinkCodeRes {
        check(telegramClient.enabled) { "텔레그램 봇이 설정되지 않았습니다." }
        var code: String
        do {
            code = String.format("%06d", random.nextInt(1_000_000))
        } while (redisTemplate.opsForValue().setIfAbsent(codeKey(code), memberId.toString(), CODE_TTL) != true)
        return TelegramLinkCodeRes(
            code = code,
            expiresAt = LocalDateTime.now().plus(CODE_TTL),
            deepLink = "https://t.me/${telegramClient.botUsername}?start=$code",
        )
    }

    @Transactional
    fun link(code: String, chatId: Long): Boolean {
        val memberId = redisTemplate.opsForValue().getAndDelete(codeKey(code))?.toLongOrNull() ?: return false
        assistantSettingRepository.findByTelegramChatId(chatId)
            ?.takeIf { it.memberId != memberId }
            ?.let { clear(it); assistantSettingRepository.saveAndFlush(it) }
        val setting = assistantSettingService.findOrDefault(memberId).apply {
            telegramChatId = chatId
            telegramStatus = TelegramStatus.ACTIVE
            telegramLinkedAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }
        assistantSettingRepository.save(setting)
        return true
    }

    fun findByChatId(chatId: Long): AssistantSetting? = assistantSettingRepository.findByTelegramChatId(chatId)

    @Transactional
    fun setStatus(memberId: Long, status: TelegramStatus): TelegramStatusRes {
        require(status == TelegramStatus.ACTIVE || status == TelegramStatus.PAUSED) { "허용되지 않는 상태입니다: $status" }
        val setting = assistantSettingService.findOrDefault(memberId)
        check(setting.telegramChatId != null) { "텔레그램이 연결되지 않았습니다." }
        setting.telegramStatus = status
        setting.updatedAt = LocalDateTime.now()
        assistantSettingRepository.save(setting)
        return status(memberId)
    }

    @Transactional
    fun pauseByChat(chatId: Long): Boolean {
        val setting = assistantSettingRepository.findByTelegramChatId(chatId) ?: return false
        setting.telegramStatus = TelegramStatus.PAUSED
        setting.updatedAt = LocalDateTime.now()
        assistantSettingRepository.save(setting)
        return true
    }

    @Transactional
    fun markBlocked(memberId: Long) {
        val setting = assistantSettingService.findOrDefault(memberId)
        if (setting.telegramChatId == null) return
        setting.telegramStatus = TelegramStatus.BLOCKED
        setting.updatedAt = LocalDateTime.now()
        assistantSettingRepository.save(setting)
        log.warn { "텔레그램 봇 차단 감지: member=$memberId" }
    }

    @Transactional
    fun unlink(memberId: Long): TelegramStatusRes {
        val setting = assistantSettingService.findOrDefault(memberId)
        clear(setting)
        assistantSettingRepository.save(setting)
        return status(memberId)
    }

    private fun clear(setting: AssistantSetting) {
        setting.telegramChatId = null
        setting.telegramStatus = TelegramStatus.NONE
        setting.telegramLinkedAt = null
        setting.updatedAt = LocalDateTime.now()
    }
}
