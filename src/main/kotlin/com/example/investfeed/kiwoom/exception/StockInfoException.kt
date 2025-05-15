package com.example.investfeed.kiwoom.exception

class StockInfoListException(): InvestFeedException(code = "INFO_0001", message = "정목 정보 리스트 조회에 실패하셨습니다.")
class StockInfoException(): InvestFeedException(code = "INFO_0002", message = "주식 기본 정보 조회에 실패하셨습니다.")
class StockInfoTradeDailyException(): InvestFeedException(code = "INFO_0003", message = "일별 거래 상세 조회에 실패하셨습니다.")
class StockInfoJumpListException(): InvestFeedException(code = "INFO_0004", message = "가격 급등락 조회에 실패하셨습니다.")