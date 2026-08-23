package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class UsExchangeRateException(): InvestFeedException(code = "US_EXCHANGE_0001", message = "미국 환전 적용환율 조회에 실패하셨습니다.")