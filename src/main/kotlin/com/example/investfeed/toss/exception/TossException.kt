package com.example.investfeed.toss.exception

import com.example.investfeed.common.exception.InvestFeedException

class TossAccessTokenNotFoundException(): InvestFeedException(code = "TOSS_TOKEN_9999", message = "토스 API 요청 토큰이 존재하지 않습니다.")
class TossAccessTokenException(): InvestFeedException(code = "TOSS_TOKEN_0001", message = "토스 API 요청 토큰 발급에 실패하였습니다.")
class TossApiException(): InvestFeedException(code = "TOSS_9999", message = "토스 API 통신 오류가 발생하였습니다.")
class TossAccountListException(): InvestFeedException(code = "TOSS_ACCOUNT_0001", message = "토스 계좌 조회에 실패하였습니다.")
class TossHoldingListException(): InvestFeedException(code = "TOSS_HOLDING_0001", message = "토스 보유 주식 조회에 실패하였습니다.")
class TossExchangeRateException(): InvestFeedException(code = "TOSS_FX_0001", message = "토스 환율 조회에 실패하였습니다.")
