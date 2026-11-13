package com.example.investfeed.domain.chart.repository

import com.example.investfeed.domain.chart.entity.ChartDrawing
import com.example.investfeed.domain.notification.entity.AssetType
import org.springframework.data.jpa.repository.JpaRepository

interface ChartDrawingRepository : JpaRepository<ChartDrawing, Long> {

    fun findByMemberIdAndAssetTypeAndAssetCode(memberId: Long, assetType: AssetType, assetCode: String): ChartDrawing?
}
