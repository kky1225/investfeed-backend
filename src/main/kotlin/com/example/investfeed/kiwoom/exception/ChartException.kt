package com.example.investfeed.kiwoom.exception

class ChartTickListException(): InvestFeedException(code = "CHART_0001", message = "주식 틱 차트 조회에 실패하셨습니다.")
class ChartMinuteListException(): InvestFeedException(code = "CHART_0002", message = "주식 분봉 차트 조회에 실패하셨습니다.")
class ChartDayListException(): InvestFeedException(code = "CHART_0003", message = "주식 일봉 차트 조회에 실패하셨습니다.")
class ChartWeekListException(): InvestFeedException(code = "CHART_0004", message = "주식 주봉 차트 조회에 실패하셨습니다.")
class ChartMonthListException(): InvestFeedException(code = "CHART_0005", message = "주식 월봉 차트 조회에 실패하셨습니다.")
class ChartYearListException(): InvestFeedException(code = "CHART_0006", message = "주식 년봉 차트 조회에 실패하셨습니다.")