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
import com.example.investfeed.domain.chart.exception.ChartInvalidRequestException
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
        val colorsValid = req.indicators.values.mapNotNull { it.colors }.all { colors ->
            colors.size <= MAX_PARAM_VALUES && colors.all { it in 0..MAX_COLOR_INDEX }
        }
        val strokesValid = req.indicators.values.all { setting ->
            (setting.widths == null ||
                (setting.widths.size <= MAX_PARAM_VALUES && setting.widths.all { it in 1..MAX_LINE_WIDTH })) &&
                (setting.dashes == null || setting.dashes.size <= MAX_PARAM_VALUES)
        }
        val paneOrderValid = req.paneOrder == null ||
            (req.paneOrder.size <= MAX_INDICATORS && req.paneOrder.all { it.isNotBlank() && it.length <= MAX_KEY_LENGTH })
        val chartTypeValid = req.chartType.isNotBlank() && req.chartType.length <= MAX_KEY_LENGTH
        val magnetValid = req.magnet.isNotBlank() && req.magnet.length <= MAX_KEY_LENGTH
        // 영역 높이는 화면이 끌어서 바꿀 수 있는 범위와 같다
        val sizesValid = req.indicators.values.mapNotNull { it.height }.all { it in INDICATOR_HEIGHT_RANGE } &&
            (req.volumeRatio == null || req.volumeRatio in VOLUME_RATIO_RANGE) &&
            (req.mainHeight == null || req.mainHeight in MAIN_HEIGHT_RANGE)
        validate(
            keysValid && paramsValid && colorsValid && strokesValid && paneOrderValid && chartTypeValid && magnetValid &&
                sizesValid
        ) {
            "차트 설정 값을 확인해주세요."
        }

        val res = ChartSettingRes(
            // 바꾼 것이 없는 지표는 저장하지 않는다
            indicators = req.indicators.filterValues {
                it.on != null || it.params != null || it.colors != null || it.widths != null || it.dashes != null ||
                    it.height != null
            },
            chartType = req.chartType,
            logScale = req.logScale,
            percentScale = req.percentScale,
            drawingToolbar = req.drawingToolbar,
            flatByPrevClose = req.flatByPrevClose,
            magnet = req.magnet,
            magnetIndicators = req.magnetIndicators,
            grid = req.grid,
            trades = req.trades,
            volumeRatio = req.volumeRatio,
            mainHeight = req.mainHeight,
            paneOrder = req.paneOrder,
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
        validate(req.drawings.size <= MAX_DRAWINGS) { "선은 종목당 최대 ${MAX_DRAWINGS}개까지 그릴 수 있습니다." }
        validate(req.drawings.all { isValid(it) }) { "그린 선의 값을 확인해주세요." }

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
        validate(assetType != AssetType.TOTAL && assetCode.isNotBlank() && assetCode.length <= MAX_ASSET_CODE_LENGTH) {
            "종목을 확인해주세요."
        }
    }

    /** 받은 값이 저장할 수 있는 것인지 확인한다. 아니면 그 이유를 화면에 보여 줄 수 있게 알린다. */
    private inline fun validate(valid: Boolean, message: () -> String) {
        if (!valid) throw ChartInvalidRequestException(message())
    }

    private fun isValid(drawing: ChartDrawingItem): Boolean {
        if (drawing.id.isBlank() || drawing.id.length > MAX_ID_LENGTH) return false
        val styleValid = (drawing.color == null || drawing.color in 0..MAX_COLOR_INDEX) &&
            (drawing.lineWidth == null || drawing.lineWidth in 1..MAX_LINE_WIDTH)
        val levelsValid = drawing.levels == null ||
            (drawing.levels.size in 1..MAX_FIBONACCI_LEVELS && drawing.levels.all { it in FIBONACCI_LEVEL_RANGE })
        if (!styleValid || !levelsValid) return false
        return when (drawing.type) {
            in TWO_POINT_TYPES -> isValid(drawing.from) && isValid(drawing.to)
            TYPE_CHANNEL -> isValid(drawing.from) && isValid(drawing.to) && isValid(drawing.third)
            TYPE_TEXT -> isValid(drawing.from) && !drawing.text.isNullOrBlank() && drawing.text.length <= MAX_TEXT_LENGTH
            TYPE_HORIZONTAL -> drawing.price?.isFinite() == true
            TYPE_VERTICAL -> (drawing.time ?: 0) > 0 && isValidOffset(drawing.offset)
            else -> false
        }
    }

    private fun isValid(anchor: ChartAnchor?): Boolean =
        anchor != null && anchor.time > 0 && anchor.price.isFinite() && isValidOffset(anchor.offset)

    private fun isValidOffset(offset: Int?): Boolean = offset == null || offset in 0..MAX_FUTURE_OFFSET

    companion object {
        private const val MAX_INDICATORS = 50       // 설정을 바꿔 둘 수 있는 지표의 최대 개수
        private val LEGACY_FIELDS = listOf("hidden", "shown", "params")     // 예전 형태에만 있는 항목
        private const val TOOLBAR_KEY = "drawingToolbar"    // 예전 형태에서 그리기 도구 모음을 숨겼다는 표시
        private const val MAX_PARAM_VALUES = 10     // 지표 하나의 설정값 최대 개수
        private const val MAX_PARAM_VALUE = 1000.0
        private const val MAX_KEY_LENGTH = 30
        private val INDICATOR_HEIGHT_RANGE = 50..300    // 보조지표 영역 하나의 높이(px)
        private val VOLUME_RATIO_RANGE = 0.1..0.5       // 가격+거래량 높이 중 거래량 영역의 비율
        private val MAIN_HEIGHT_RANGE = 250..800        // 가격+거래량 영역의 높이(px)
        private const val MAX_DRAWINGS = 200
        private const val MAX_ID_LENGTH = 40
        private const val MAX_ASSET_CODE_LENGTH = 50
        // 두 점(from·to)으로 정해지는 도형
        private val TWO_POINT_TYPES = setOf("trend", "ray", "rectangle", "fibonacci", "measure")
        private const val MAX_FIBONACCI_LEVELS = 20         // 피보나치 되돌림 하나에 켜 둘 수 있는 비율의 수
        private val FIBONACCI_LEVEL_RANGE = 0.0..10.0       // 비율 하나의 범위 (확장 비율 포함)
        private const val TYPE_CHANNEL = "channel"
        private const val TYPE_TEXT = "text"
        private const val MAX_TEXT_LENGTH = 300     // 줄 바꿈 포함
        private const val TYPE_HORIZONTAL = "horizontal"
        private const val TYPE_VERTICAL = "vertical"
        private const val MAX_COLOR_INDEX = 20
        private const val MAX_LINE_WIDTH = 5
        private const val MAX_FUTURE_OFFSET = 10_000    // 미래 구간에 놓을 수 있는 최대 칸 수
        private val DRAWING_LIST_TYPE = object : TypeReference<List<ChartDrawingItem>>() {}
    }
}
