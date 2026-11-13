package com.example.investfeed.domain.chart.dto.res

import com.example.investfeed.domain.chart.dto.ChartIndicatorSetting

/** 회원의 차트 설정. 저장한 적이 없으면 전부 기본값(지표는 빈 목록) */
data class ChartSettingRes(
    // 지표별로 기본 상태에서 바꾼 것. key 는 지표 이름. 없는 지표는 화면이 기본값을 쓴다
    val indicators: Map<String, ChartIndicatorSetting> = emptyMap(),
    val drawingToolbar: Boolean = true,         // 그리기 도구 모음 표시
    val flatByPrevClose: Boolean = false,       // 시가와 종가가 같은 캔들의 색 기준. false 면 상승색, true 면 전일 종가와 비교
)
