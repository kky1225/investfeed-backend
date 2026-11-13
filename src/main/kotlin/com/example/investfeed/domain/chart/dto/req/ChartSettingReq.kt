package com.example.investfeed.domain.chart.dto.req

import com.example.investfeed.domain.chart.dto.ChartIndicatorSetting

data class ChartSettingReq(
    // 지표별로 기본 상태에서 바꾼 것. key 는 지표 이름(average, rsi, macd, bollinger 등). 바꾸지 않은 지표는 넣지 않는다
    val indicators: Map<String, ChartIndicatorSetting> = emptyMap(),
    val drawingToolbar: Boolean = true,         // 그리기 도구 모음 표시
    val flatByPrevClose: Boolean = false,       // 시가와 종가가 같은 캔들의 색을 전일 종가와 비교해 정할지
)
