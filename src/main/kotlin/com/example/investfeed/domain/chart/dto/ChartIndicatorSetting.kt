package com.example.investfeed.domain.chart.dto

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 지표 하나에 대해 회원이 기본 상태에서 바꾼 것. 요청·응답·저장에 같은 모양을 쓴다.
 * 바꾸지 않은 항목은 null 이고, 화면이 그 지표의 기본값을 쓴다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChartIndicatorSetting(
    val on: Boolean? = null,            // 켜짐 여부. null 이면 지표의 기본 상태
    val params: List<Double>? = null,   // 설정값 (기간 등). 순서는 설정 창의 칸 순서. null 이면 기본값
)
