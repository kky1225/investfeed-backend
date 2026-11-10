package com.example.investfeed.upbit.exception

import com.example.investfeed.common.exception.ExternalApiException

class UpbitApiException : ExternalApiException(code = "UPBIT_9999", message = "업비트 API 통신 오류가 발생하였습니다.")
