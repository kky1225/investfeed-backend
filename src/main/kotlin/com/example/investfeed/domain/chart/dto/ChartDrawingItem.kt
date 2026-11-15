package com.example.investfeed.domain.chart.dto

import com.fasterxml.jackson.annotation.JsonInclude

/** 도형의 점이 붙는 자리. 화면 좌표가 아니라 캔들 시각과 가격 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChartAnchor(
    val time: Long,     // 캔들 시각 (epoch ms)
    val price: Double,
    val offset: Int? = null,    // time 의 캔들에서 뒤로 몇 칸인지. 마지막 캔들보다 오른쪽(미래 구간)에 놓은 점에만 있다
)

/**
 * 차트에 그린 도형 한 개. 요청·응답·저장에 같은 모양을 쓴다.
 * type 에 따라 쓰는 값이 다르다:
 * trend(추세선)·ray(반직선)·rectangle(사각형) = from·to / channel(평행 채널) = from·to·third
 * / horizontal(수평선) = price / vertical(수직선) = time(·offset)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChartDrawingItem(
    val id: String,
    val type: String,
    val from: ChartAnchor? = null,
    val to: ChartAnchor? = null,
    val third: ChartAnchor? = null,     // 평행 채널에서 맞은편 선이 지나는 점
    val price: Double? = null,
    val time: Long? = null,             // 수직선이 놓인 캔들 시각 (epoch ms)
    val offset: Int? = null,            // 수직선이 미래 구간에 있을 때, time 의 캔들에서 뒤로 몇 칸인지
    // 꾸미기. 없으면 화면의 기본값
    val color: Int? = null,             // 화면이 가진 선 색 목록에서의 순번
    val lineWidth: Int? = null,
    val dashed: Boolean? = null,
)
