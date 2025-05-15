package com.example.investfeed.kiwoom.exception

class InvestorTradeDailyException(): InvestFeedException(code = "INVESTOR_0001", message = "투자자별 일별 매매 조회에 실패하셨습니다.")
class InvestorTradeOrganizeException(): InvestFeedException(code = "INVESTOR_0002", message = "종목별 투자자 기관별 합계 요청 조회에 실패하셨습니다.")
class InvestorTradeRankListException(): InvestFeedException(code = "INVESTOR_0003", message = "기관/외국인 매수 상위 순위 조회에 실패하셨습니다.")