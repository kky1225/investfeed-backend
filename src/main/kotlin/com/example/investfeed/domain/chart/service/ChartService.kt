package com.example.investfeed.domain.chart.service

import com.example.investfeed.domain.chart.dto.ChartAnchor
import com.example.investfeed.domain.chart.dto.ChartDrawingItem
import com.example.investfeed.domain.chart.dto.req.ChartDrawingSaveReq
import com.example.investfeed.domain.chart.dto.req.ChartSettingReq
import com.example.investfeed.domain.chart.dto.res.ChartDrawingRes
import com.example.investfeed.domain.chart.dto.res.ChartSettingRes
import com.example.investfeed.domain.chart.entity.ChartDrawing
import com.example.investfeed.domain.chart.entity.ChartSetting
import com.example.investfeed.domain.chart.repository.ChartDrawingRepository
import com.example.investfeed.domain.chart.repository.ChartSettingRepository
import com.example.investfeed.domain.notification.entity.AssetType
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** 회원별 차트 설정과 종목별로 그린 선을 저장한다. */
@Service
class ChartService(
    private val chartSettingRepository: ChartSettingRepository,
    private val chartDrawingRepository: ChartDrawingRepository,
    private val objectMapper: ObjectMapper,
) {

    fun getSetting(memberId: Long): ChartSettingRes =
        chartSettingRepository.findByMemberId(memberId)
            ?.let { objectMapper.readValue(it.settings, ChartSettingRes::class.java) }
            ?: ChartSettingRes()

    @Transactional
    fun saveSetting(memberId: Long, req: ChartSettingReq): ChartSettingRes {
        val hidden = req.hidden.distinct()
        require(hidden.size <= MAX_HIDDEN && hidden.all { it.isNotBlank() && it.length <= MAX_KEY_LENGTH }) {
            "차트 설정 값을 확인해주세요."
        }

        val res = ChartSettingRes(hidden = hidden, flatByPrevClose = req.flatByPrevClose)
        val json = objectMapper.writeValueAsString(res)
        val existing = chartSettingRepository.findByMemberId(memberId)
        if (existing != null) {
            existing.settings = json
            existing.updatedAt = LocalDateTime.now()
        } else {
            chartSettingRepository.save(ChartSetting(memberId = memberId, settings = json))
        }
        return res
    }

    fun getDrawings(memberId: Long, assetType: AssetType, assetCode: String): ChartDrawingRes {
        validateAsset(assetType, assetCode)
        val drawings = chartDrawingRepository.findByMemberIdAndAssetTypeAndAssetCode(memberId, assetType, assetCode)
            ?.let { objectMapper.readValue(it.drawings, DRAWING_LIST_TYPE) }
            ?: emptyList()
        return ChartDrawingRes(drawings = drawings)
    }

    /** 이 종목의 선 전체를 받은 목록으로 바꾼다. 빈 목록이면 행을 지운다. */
    @Transactional
    fun saveDrawings(memberId: Long, assetType: AssetType, assetCode: String, req: ChartDrawingSaveReq): ChartDrawingRes {
        validateAsset(assetType, assetCode)
        require(req.drawings.size <= MAX_DRAWINGS) { "선은 종목당 최대 ${MAX_DRAWINGS}개까지 그릴 수 있습니다." }
        require(req.drawings.all { isValid(it) }) { "그린 선의 값을 확인해주세요." }

        val existing = chartDrawingRepository.findByMemberIdAndAssetTypeAndAssetCode(memberId, assetType, assetCode)
        if (req.drawings.isEmpty()) {
            existing?.let { chartDrawingRepository.delete(it) }
            return ChartDrawingRes(drawings = emptyList())
        }

        val json = objectMapper.writeValueAsString(req.drawings)
        if (existing != null) {
            existing.drawings = json
            existing.updatedAt = LocalDateTime.now()
        } else {
            chartDrawingRepository.save(
                ChartDrawing(memberId = memberId, assetType = assetType, assetCode = assetCode, drawings = json)
            )
        }
        return ChartDrawingRes(drawings = req.drawings)
    }

    private fun validateAsset(assetType: AssetType, assetCode: String) {
        require(assetType != AssetType.TOTAL && assetCode.isNotBlank() && assetCode.length <= MAX_ASSET_CODE_LENGTH) {
            "종목을 확인해주세요."
        }
    }

    private fun isValid(drawing: ChartDrawingItem): Boolean {
        if (drawing.id.isBlank() || drawing.id.length > MAX_ID_LENGTH) return false
        return when (drawing.type) {
            TYPE_TREND -> isValid(drawing.from) && isValid(drawing.to)
            TYPE_HORIZONTAL -> drawing.price?.isFinite() == true
            else -> false
        }
    }

    private fun isValid(anchor: ChartAnchor?): Boolean =
        anchor != null && anchor.time > 0 && anchor.price.isFinite()

    companion object {
        private const val MAX_HIDDEN = 50
        private const val MAX_KEY_LENGTH = 30
        private const val MAX_DRAWINGS = 200
        private const val MAX_ID_LENGTH = 40
        private const val MAX_ASSET_CODE_LENGTH = 50
        private const val TYPE_TREND = "trend"
        private const val TYPE_HORIZONTAL = "horizontal"
        private val DRAWING_LIST_TYPE = object : TypeReference<List<ChartDrawingItem>>() {}
    }
}
