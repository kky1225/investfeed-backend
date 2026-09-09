package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class UsHoldingListException(): InvestFeedException(code = "US_HOLDING_0001", message = "미국주식 보유종목 조회에 실패하셨습니다.")
class UsDepositException(): InvestFeedException(code = "US_HOLDING_0002", message = "해외주식 예수금 조회에 실패하셨습니다.")
