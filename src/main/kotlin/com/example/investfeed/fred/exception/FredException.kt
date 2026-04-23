package com.example.investfeed.fred.exception

import com.example.investfeed.common.exception.InvestFeedException

class FredApiException : InvestFeedException(code = "FRED_9999", message = "FRED API 통신 오류가 발생하였습니다.")
class FredSeriesObservationsException : InvestFeedException(code = "FRED_0001", message = "FRED 시리즈 관측값 조회에 실패하였습니다.")
class FredReleaseDatesException : InvestFeedException(code = "FRED_0002", message = "FRED 발표일 조회에 실패하였습니다.")
