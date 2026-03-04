package com.example.investfeed.kiwoom.exception

class InvestorTradeOpenMarketException(): InvestFeedException(code = "PRICE_0001", message = "장중 투자자별 매매 조회에 실패하였습니다.")
class InvestorTradeCloseMarketException(): InvestFeedException(code = "PRICE_0002", message = "장마감 후 투자자별 매매 조회에 실패하였습니다.")
class ProgramTradeException(): InvestFeedException(code = "PRICE_0003", message = "프로그램 매매 추이 조회에 실패하였습니다.")