package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.ExternalApiException

class StockChartMinuteListException(): ExternalApiException(code = "CHART_0002", message = "주식 분봉 차트 조회에 실패하셨습니다.")
class StockChartDayListException(): ExternalApiException(code = "CHART_0003", message = "주식 일봉 차트 조회에 실패하셨습니다.")
class StockChartWeekListException(): ExternalApiException(code = "CHART_0004", message = "주식 주봉 차트 조회에 실패하셨습니다.")
class StockChartMonthListException(): ExternalApiException(code = "CHART_0005", message = "주식 월봉 차트 조회에 실패하셨습니다.")
class StockChartYearListException(): ExternalApiException(code = "CHART_0006", message = "주식 년봉 차트 조회에 실패하셨습니다.")
class StockChartPageException(): ExternalApiException(code = "CHART_0007", message = "주식 과거 차트 조회에 실패하셨습니다.")
class StockChartInvestorException(): ExternalApiException(code = "STOCK_CHART_0006", message = "장중 투자자별 매매 차트 조회에 실패하셨습니다.")

class SectChartMinuteListException(): ExternalApiException(code = "SECT_CHART_0001", message = "업종 분봉 차트 조회에 실패하셨습니다.")
class SectChartDayListException(): ExternalApiException(code = "SECT_CHART_0002", message = "업종 일봉 차트 조회에 실패하셨습니다.")
class SectChartWeekListException(): ExternalApiException(code = "SECT_CHART_0003", message = "업종 주봉 차트 조회에 실패하셨습니다.")
class SectChartMonthListException(): ExternalApiException(code = "SECT_CHART_0004", message = "업종 월봉 차트 조회에 실패하셨습니다.")
class SectChartYearListException(): ExternalApiException(code = "SECT_CHART_0005", message = "업종 년봉 차트 조회에 실패하셨습니다.")
