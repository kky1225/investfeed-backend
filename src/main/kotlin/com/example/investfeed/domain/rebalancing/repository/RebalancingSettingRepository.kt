package com.example.investfeed.domain.rebalancing.repository

import com.example.investfeed.domain.rebalancing.entity.RebalancingSetting
import org.springframework.data.jpa.repository.JpaRepository

interface RebalancingSettingRepository : JpaRepository<RebalancingSetting, Long> {

    fun findByMemberId(memberId: Long): RebalancingSetting?
}
