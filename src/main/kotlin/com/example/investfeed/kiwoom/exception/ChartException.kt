package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class StockChartMinuteListException(): InvestFeedException(code = "CHART_0002", message = "주식 분봉 차트 조회에 실패하셨습니다.")
class StockChartDayListException(): InvestFeedException(code = "CHART_0003", message = "주식 일봉 차트 조회에 실패하셨습니다.")
class StockChartWeekListException(): InvestFeedException(code = "CHART_0004", message = "주식 주봉 차트 조회에 실패하셨습니다.")
class StockChartMonthListException(): InvestFeedException(code = "CHART_0005", message = "주식 월봉 차트 조회에 실패하셨습니다.")
class StockChartYearListException(): InvestFeedException(code = "CHART_0006", message = "주식 년봉 차트 조회에 실패하셨습니다.")
class StockChartInvestorException(): InvestFeedException(code = "STOCK_CHART_0006", message = "장중 투자자별 매매 차트 조회에 실패하셨습니다.")

class SectChartMinuteListException(): InvestFeedException(code = "SECT_CHART_0001", message = "업종 분봉 차트 조회에 실패하셨습니다.")
class SectChartDayListException(): InvestFeedException(code = "SECT_CHART_0002", message = "업종 일봉 차트 조회에 실패하셨습니다.")
class SectChartWeekListException(): InvestFeedException(code = "SECT_CHART_0003", message = "업종 주봉 차트 조회에 실패하셨습니다.")
class SectChartMonthListException(): InvestFeedException(code = "SECT_CHART_0004", message = "업종 월봉 차트 조회에 실패하셨습니다.")
class SectChartYearListException(): InvestFeedException(code = "SECT_CHART_0005", message = "업종 년봉 차트 조회에 실패하셨습니다.")
