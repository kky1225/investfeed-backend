package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class StockInfoListException(): InvestFeedException(code = "STOCK_0001", message = "정목 정보 리스트 조회에 실패하셨습니다.")
class StockTradeInfoException(): InvestFeedException(code = "STOCK_0002", message = "주식 시분 요청에 실패하셨습니다.")
class StockDefaultInfoException(): InvestFeedException(code = "STOCK_0003", message = "주식 기본 정보 조회에 실패하셨습니다.")
class StockInfoException(): InvestFeedException(code = "STOCK_0004", message = "주식 정보 조회에 실패하셨습니다.")
class StockInvestorException(): InvestFeedException(code = "STOCK_0005", message = "종목별 투자자기관별 요청에 실패하셨습니다.")
class StockTradeDailyListException(): InvestFeedException(code = "STOCK_0006", message = "일별 거래 상세 조회에 실패하셨습니다.")
class StockJumpListException(): InvestFeedException(code = "STOCK_0007", message = "가격 급등락 조회에 실패하셨습니다.")
class StockSinglePriceListException(): InvestFeedException(code = "STOCK_0008", message = "시간외단일가 조회에 실패하셨습니다.")
class StockNewPriceListException(): InvestFeedException(code = "STOCK_0009", message = "신고저가 조회에 실패하셨습니다.")
class StockTradeValueListException(): InvestFeedException(code = "STOCK_0010", message = "거래대금 상위 요청 조회에 실패하셨습니다.")
class StockTradeVolumeListException(): InvestFeedException(code = "STOCK_0011", message = "당일 거래량 상위 순위 조회에 실패하셨습니다.")
class StockSurgeTradeVolumeListException(): InvestFeedException(code = "STOCK_0012", message = "거래량 급증 순위 조회에 실패하셨습니다.")
class StockInterestException(): InvestFeedException(code = "STOCK_0012", message = "관심종목 조회에 실패하셨습니다.")