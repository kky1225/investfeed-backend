package com.example.investfeed.kiwoom.exception

class InvestorTradeOpenMarketException(): InvestFeedException(code = "PRICE_0001", message = "장중 투자자별 매매 조회에 실패하였습니다.")
class InvestorTradeCloseMarketException(): InvestFeedException(code = "PRICE_0002", message = "장마감 후 투자자별 매매 조회에 실패하였습니다.")
class ProgramTradeException(): InvestFeedException(code = "PRICE_0003", message = "프로그램 매매 추이 조회에 실패하였습니다.")
class StockProgramTradeDayException(): InvestFeedException(code = "PRICE_0004", message = "종목 일별 프로그램 매매 추이 조회에 실패하였습니다.")
class IndexProgramTradeDayException(): InvestFeedException(code = "PRICE_0005", message = "지수 일별 프로그램 매매 추이 조회에 실패하였습니다.")
class IndexProgramTradeMinuteException(): InvestFeedException(code = "PRICE_0006", message = "지수 시간대별 프로그램 매매 추이 조회에 실패하였습니다.")