package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.ExternalApiException

class GoldPriceNowException(): ExternalApiException(code = "GOLD_0001", message = "금현물 시세 정보 조회에 실패하였습니다.")
class GoldPriceNowMinuteException(): ExternalApiException(code = "GOLD_0002", message = "금현물 호가 정보 조회에 실패하였습니다.")
class GoldInvestorException(): ExternalApiException(code = "GOLD_0003", message = "금현물 투자자 현황 정보 조회에 실패하였습니다.")

class GoldChartMinuteListException(): ExternalApiException(code = "GOLD_0004", message = "금현물 분봉 차트 조회에 실패하셨습니다.")
class GoldChartDayListException(): ExternalApiException(code = "GOLD_0005", message = "금현물 일봉 차트 조회에 실패하셨습니다.")
class GoldChartWeekListException(): ExternalApiException(code = "GOLD_0006", message = "금현물 주봉 차트 조회에 실패하셨습니다.")
class GoldChartMonthListException(): ExternalApiException(code = "GOLD_0007", message = "금현물 월봉 차트 조회에 실패하셨습니다.")