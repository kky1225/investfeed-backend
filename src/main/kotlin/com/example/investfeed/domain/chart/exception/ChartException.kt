package com.example.investfeed.domain.chart.exception

import com.example.investfeed.common.exception.InvestFeedException

/** 저장할 수 없는 차트 설정·그린 선·종목을 받았을 때. 메시지를 화면에 그대로 보여 준다. */
class ChartInvalidRequestException(message: String) : InvestFeedException(code = "CHART_4001", message = message)
