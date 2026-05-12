package com.example.investfeed.domain.recommend.service

import com.example.investfeed.domain.recommend.dto.req.RecommendSettingReq
import com.example.investfeed.domain.recommend.dto.res.RecommendSettingRes
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.RiskPreset
import com.example.investfeed.domain.recommend.repository.RecommendSettingRepository
import com.example.investfeed.domain.security.CustomUserDetails
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RecommendSettingService(
    private val recommendSettingRepository: RecommendSettingRepository,
) {

    fun getSetting(): RecommendSettingRes {
        val memberId = getMemberId()
        val setting = recommendSettingRepository.findByMemberId(memberId)
        return if (setting != null) toRes(setting) else defaultSetting()
    }

    fun getPresetByMemberId(memberId: Long): RiskPreset {
        return recommendSettingRepository.findByMemberId(memberId)?.riskPreset ?: RiskPreset.NORMAL
    }

    @Transactional
    fun saveSetting(req: RecommendSettingReq): RecommendSettingRes {
        val memberId = getMemberId()
        val existing = recommendSettingRepository.findByMemberId(memberId)

        val setting = if (existing != null) {
            existing.riskPreset = req.riskPreset
            existing.updatedAt = LocalDateTime.now()
            existing
        } else {
            recommendSettingRepository.save(
                RecommendSetting(
                    memberId = memberId,
                    riskPreset = req.riskPreset,
                )
            )
        }

        return toRes(setting)
    }

    private fun toRes(setting: RecommendSetting): RecommendSettingRes {
        return RecommendSettingRes(riskPreset = setting.riskPreset)
    }

    private fun defaultSetting(): RecommendSettingRes {
        return RecommendSettingRes(riskPreset = RiskPreset.NORMAL)
    }

    private fun getMemberId(): Long {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
    }
}
