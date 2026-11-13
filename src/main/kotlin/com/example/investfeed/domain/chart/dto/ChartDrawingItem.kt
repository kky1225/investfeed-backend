package com.example.investfeed.domain.chart.dto

import com.fasterxml.jackson.annotation.JsonInclude

/** 선이 붙는 자리. 화면 좌표가 아니라 캔들 시각과 가격 */
data class ChartAnchor(
    val time: Long,     // 캔들 시각 (epoch ms)
    val price: Double,
)

/**
 * 차트에 그린 선 한 개. 요청·응답·저장에 같은 모양을 쓴다.
 * type 에 따라 쓰는 값이 다르다: trend(추세선)=from·to, horizontal(수평선)=price
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChartDrawingItem(
    val id: String,
    val type: String,
    val from: ChartAnchor? = null,
    val to: ChartAnchor? = null,
    val price: Double? = null,
)
