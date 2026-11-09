package com.example.investfeed.global.holiday.exception

import com.example.investfeed.common.exception.ExternalApiException

class HolidayApiException : ExternalApiException(code = "HOLIDAY_9999", message = "공휴일 API 통신 오류가 발생하였습니다.")
class HolidayInfoException : ExternalApiException(code = "HOLIDAY_0001", message = "공휴일 정보 조회에 실패하였습니다.")
