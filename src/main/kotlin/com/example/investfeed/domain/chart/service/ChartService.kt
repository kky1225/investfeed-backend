package com.example.investfeed.domain.chart.service

import com.example.investfeed.domain.chart.dto.ChartAnchor
import com.example.investfeed.domain.chart.dto.ChartDrawingItem
import com.example.investfeed.domain.chart.dto.ChartIndicatorSetting
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
            ?.let { readSetting(it.settings) }
            ?: ChartSettingRes()

    /**
     * 저장된 설정을 읽는다. 예전 형태(끈 항목 목록 hidden, 켠 항목 목록 shown, 지표별 설정값 params 가 따로 있던 것)로
     * 저장된 행은 지표별 묶음으로 바꿔 돌려준다. 다음에 저장할 때 새 형태로 덮어써진다.
     */
    private fun readSetting(json: String): ChartSettingRes {
        val node = objectMapper.readTree(json)
        val isLegacy = LEGACY_FIELDS.any { node.has(it) }
        if (!isLegacy) return objectMapper.treeToValue(node, ChartSettingRes::class.java)

        val hidden = node.path("hidden").mapNotNull { it.asText(null) }
        val shown = node.path("shown").mapNotNull { it.asText(null) }
        val params = node.path("params").properties().associate { (key, values) -> key to values.map { it.asDouble() } }
        val indicatorKeys = (hidden + shown + params.keys).distinct() - TOOLBAR_KEY
        return ChartSettingRes(
            indicators = indicatorKeys.associateWith { key ->
                ChartIndicatorSetting(
                    on = when (key) {
                        in hidden -> false
                        in shown -> true
                        else -> null
                    },
                    params = params[key],
                )
            },
            drawingToolbar = TOOLBAR_KEY !in hidden,
            flatByPrevClose = node.path("flatByPrevClose").asBoolean(false),
        )
    }

    @Transactional
    fun saveSetting(memberId: Long, req: ChartSettingReq): ChartSettingRes {
        // 지표가 실제로 있는지, 값이 그 지표에 맞는지는 화면이 판단한다(맞지 않으면 기본값을 쓴다).
        // 여기서는 저장할 수 있는 크기와 숫자 범위만 막는다.
        val keysValid = req.indicators.size <= MAX_INDICATORS &&
            req.indicators.keys.all { it.isNotBlank() && it.length <= MAX_KEY_LENGTH }
        val paramsValid = req.indicators.values.mapNotNull { it.params }.all { values ->
            values.size <= MAX_PARAM_VALUES && values.all { it.isFinite() && it > 0 && it <= MAX_PARAM_VALUE }
        }
        require(keysValid && paramsValid) { "차트 설정 값을 확인해주세요." }

        val res = ChartSettingRes(
            // 바꾼 것이 없는 지표는 저장하지 않는다
            indicators = req.indicators.filterValues { it.on != null || it.params != null },
            drawingToolbar = req.drawingToolbar,
            flatByPrevClose = req.flatByPrevClose,
        )
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
        private const val MAX_INDICATORS = 50       // 설정을 바꿔 둘 수 있는 지표의 최대 개수
        private val LEGACY_FIELDS = listOf("hidden", "shown", "params")     // 예전 형태에만 있는 항목
        private const val TOOLBAR_KEY = "drawingToolbar"    // 예전 형태에서 그리기 도구 모음을 숨겼다는 표시
        private const val MAX_PARAM_VALUES = 10     // 지표 하나의 설정값 최대 개수
        private const val MAX_PARAM_VALUE = 1000.0
        private const val MAX_KEY_LENGTH = 30
        private const val MAX_DRAWINGS = 200
        private const val MAX_ID_LENGTH = 40
        private const val MAX_ASSET_CODE_LENGTH = 50
        private const val TYPE_TREND = "trend"
        private const val TYPE_HORIZONTAL = "horizontal"
        private val DRAWING_LIST_TYPE = object : TypeReference<List<ChartDrawingItem>>() {}
    }
}
