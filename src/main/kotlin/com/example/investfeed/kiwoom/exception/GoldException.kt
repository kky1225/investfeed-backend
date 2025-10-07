package com.example.investfeed.kiwoom.exception

class GoldPriceNowException(): InvestFeedException(code = "GOLD_0001", message = "금현물 시세 정보 조회에 실패하였습니다.")
class GoldInvestorException(): InvestFeedException(code = "GOLD_0002", message = "금현물 투자자 현황 정보 조회에 실패하였습니다.")