package com.example.investfeed.domain.calendar.exception

import com.example.investfeed.common.exception.InvestFeedException

class MarketSessionNotFoundException : InvestFeedException(code = "CALENDAR_4041", message = "장 시각 변경일을 찾을 수 없습니다.")
class MarketSessionDuplicateException : InvestFeedException(code = "CALENDAR_4091", message = "이미 등록된 날짜입니다.")
class MarketSessionInvalidTimeException : InvestFeedException(code = "CALENDAR_4001", message = "마감 시각은 개장 시각보다 늦어야 합니다.")
class MarketSessionTimeRangeException(message: String) : InvestFeedException(code = "CALENDAR_4002", message = message)
