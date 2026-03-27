package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class GoldPriceNowException(): InvestFeedException(code = "GOLD_0001", message = "금현물 시세 정보 조회에 실패하였습니다.")
class GoldPriceNowMinuteException(): InvestFeedException(code = "GOLD_0002", message = "금현물 호가 정보 조회에 실패하였습니다.")
class GoldInvestorException(): InvestFeedException(code = "GOLD_0003", message = "금현물 투자자 현황 정보 조회에 실패하였습니다.")

class GoldChartMinuteListException(): InvestFeedException(code = "GOLD_0004", message = "금현물 분봉 차트 조회에 실패하셨습니다.")
class GoldChartDayListException(): InvestFeedException(code = "GOLD_0005", message = "금현물 일봉 차트 조회에 실패하셨습니다.")
class GoldChartWeekListException(): InvestFeedException(code = "GOLD_0006", message = "금현물 주봉 차트 조회에 실패하셨습니다.")
class GoldChartMonthListException(): InvestFeedException(code = "GOLD_0007", message = "금현물 월봉 차트 조회에 실패하셨습니다.")