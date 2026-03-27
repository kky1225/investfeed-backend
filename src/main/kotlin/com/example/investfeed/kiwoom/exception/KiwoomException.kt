package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class AccessTokenNotFoundException(): InvestFeedException(code = "TOKEN_9999", message = "API 요청 토큰이 존재하지 않습니다.")
class KiwoomApiException(): InvestFeedException(code = "KIWOOM_9999", message = "API 통신 오류가 발생하였습니다.")