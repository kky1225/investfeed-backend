package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.req.AssistantSettingReq
import com.example.investfeed.domain.assistant.dto.res.AssistantSettingRes
import com.example.investfeed.domain.assistant.entity.AssistantSetting
import com.example.investfeed.domain.assistant.entity.AssistantSettingSectionOff
import com.example.investfeed.domain.assistant.repository.AssistantSettingRepository
import com.example.investfeed.domain.assistant.repository.AssistantSettingSectionOffRepository
import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AssistantSettingService(
    private val assistantSettingRepository: AssistantSettingRepository,
    private val memberBrokerRepository: MemberBrokerRepository,
    private val sectionOffRepository: AssistantSettingSectionOffRepository,
) {

    fun getSetting(memberId: Long): AssistantSettingRes = toRes(findOrDefault(memberId))

    fun findOrDefault(memberId: Long): AssistantSetting =
        assistantSettingRepository.findById(memberId).orElseGet {
            AssistantSetting(memberId = memberId, coinEnabled = memberBrokerRepository.findByMemberIdAndBrokerMarketOrderByOrderIndex(memberId, MarketType.CRYPTO).isNotEmpty())
        }

    fun enabledFor(setting: AssistantSetting, type: BriefingType): Boolean = when (type) {
        BriefingType.KR_PRE, BriefingType.KR_CLOSE, BriefingType.KR_HOLDINGS -> setting.krEnabled
        BriefingType.US_CLOSE -> setting.usEnabled
        BriefingType.COIN_DAILY -> setting.coinEnabled
    }

    fun sectionsOff(setting: AssistantSetting): Set<String> =
        sectionOffRepository.findByMemberId(setting.memberId).map { it.sectionId }.toSet()

    @Transactional
    fun saveSetting(memberId: Long, req: AssistantSettingReq): AssistantSettingRes {
        val setting = findOrDefault(memberId).apply {
            krEnabled = req.krEnabled
            usEnabled = req.usEnabled
            coinEnabled = req.coinEnabled
            krWarnEnabled = req.krWarnEnabled
            usWarnEnabled = req.usWarnEnabled
            releaseAlertEnabled = req.releaseAlertEnabled
            updatedAt = LocalDateTime.now()
        }
        val saved = assistantSettingRepository.save(setting)
        val off = req.sectionsOff.distinct()
        sectionOffRepository.deleteByMemberId(memberId)
        sectionOffRepository.flush()
        sectionOffRepository.saveAll(off.map { AssistantSettingSectionOff(memberId, it) })
        return toRes(saved, off)
    }

    @Transactional
    fun markRead(memberId: Long, lastSeenId: Long) {
        val setting = findOrDefault(memberId)
        if (lastSeenId <= setting.lastSeenMessageId) return
        setting.lastSeenMessageId = lastSeenId
        setting.updatedAt = LocalDateTime.now()
        assistantSettingRepository.save(setting)
    }

    private fun toRes(setting: AssistantSetting, off: List<String>? = null) = AssistantSettingRes(
        krEnabled = setting.krEnabled,
        usEnabled = setting.usEnabled,
        coinEnabled = setting.coinEnabled,
        krWarnEnabled = setting.krWarnEnabled,
        usWarnEnabled = setting.usWarnEnabled,
        releaseAlertEnabled = setting.releaseAlertEnabled,
        sectionsOff = off ?: sectionsOff(setting).toList(),
    )
}
