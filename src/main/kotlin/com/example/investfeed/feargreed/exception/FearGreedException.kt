package com.example.investfeed.feargreed.exception

import com.example.investfeed.kiwoom.exception.InvestFeedException

class FearGreedApiException : InvestFeedException(code = "FEAR_GREED_9999", message = "공포탐욕지수 API 통신 오류가 발생하였습니다.")
