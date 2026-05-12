package com.example.investfeed.domain.recommend.repository

import com.example.investfeed.domain.recommend.entity.RecommendSetting
import org.springframework.data.jpa.repository.JpaRepository

interface RecommendSettingRepository : JpaRepository<RecommendSetting, Long> {
    fun findByMemberId(memberId: Long): RecommendSetting?
}