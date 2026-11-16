package com.example.investfeed.domain.chart.dto.res

import com.example.investfeed.domain.chart.dto.ChartIndicatorSetting

/** 회원의 차트 설정. 저장한 적이 없으면 전부 기본값(지표는 빈 목록) */
data class ChartSettingRes(
    // 지표별로 기본 상태에서 바꾼 것. key 는 지표 이름. 없는 지표는 화면이 기본값을 쓴다
    val indicators: Map<String, ChartIndicatorSetting> = emptyMap(),
    val chartType: String = "candle",           // 가격을 그리는 방식 (candle, bar, line, area, heikinAshi). 화면이 모르는 값이면 캔들로 그린다
    val logScale: Boolean = false,              // 가격축을 로그로
    val percentScale: Boolean = false,          // 가격축에 가격 대신 등락률(%)을 적을지. 기준은 화면에 보이는 첫 캔들의 종가
    val drawingToolbar: Boolean = true,         // 그리기 도구 모음 표시
    val flatByPrevClose: Boolean = false,       // 시가와 종가가 같은 캔들의 색 기준. false 면 상승색, true 면 전일 종가와 비교
    val magnet: String = "off",                 // 자석: 도형의 점을 캔들의 시가·고가·저가·종가에 붙이는 정도 (off, weak, strong). 화면이 모르는 값이면 끈 것으로 본다
    val magnetIndicators: Boolean = false,      // 자석을 가격 영역의 지표 선(이동평균선 등)에도 붙일지
    val volumeRatio: Double? = null,           // 가격+거래량 높이 중 거래량 영역의 비율. null 이면 기본 비율
    val mainHeight: Int? = null,                // 가격+거래량 영역의 높이(px). null 이면 화면이 정한 높이
)
