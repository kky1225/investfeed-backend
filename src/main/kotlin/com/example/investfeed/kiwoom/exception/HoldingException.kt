package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class HoldingListException(): InvestFeedException(code = "HOLDING_0001", message = "보유 주식 조회에 실패하였습니다.")
class DepositException(): InvestFeedException(code = "HOLDING_0002", message = "예수금 조회에 실패하였습니다.")
