package com.example.investfeed.kiwoom.exception

class StockInfoListException(): InvestFeedException(code = "STOCK_0001", message = "정목 정보 리스트 조회에 실패하셨습니다.")
class StockInfoException(): InvestFeedException(code = "STOCK_0002", message = "주식 기본 정보 조회에 실패하셨습니다.")
class StockTradeDailyListException(): InvestFeedException(code = "STOCK_0003", message = "일별 거래 상세 조회에 실패하셨습니다.")
class StockJumpListException(): InvestFeedException(code = "STOCK_0004", message = "가격 급등락 조회에 실패하셨습니다.")
class StockSinglePriceListException(): InvestFeedException(code = "STOCK_0005", message = "시간외단일가 조회에 실패하셨습니다.")
class StockNewPriceListException(): InvestFeedException(code = "STOCK_0006", message = "신고저가 조회에 실패하셨습니다.")
class StockTradeHighListException(): InvestFeedException(code = "STOCK_0007", message = "거래대금 상위 요청 조회에 실패하셨습니다.")