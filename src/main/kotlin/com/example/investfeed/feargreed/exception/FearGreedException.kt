package com.example.investfeed.feargreed.exception

import com.example.investfeed.common.exception.InvestFeedException

class FearGreedApiException : InvestFeedException(code = "FEAR_GREED_9999", message = "공포탐욕지수 API 통신 오류가 발생하였습니다.")
class FearGreedResponseException : InvestFeedException(code = "FEAR_GREED_0001", message = "공포탐욕지수 조회에 실패하였습니다.")
