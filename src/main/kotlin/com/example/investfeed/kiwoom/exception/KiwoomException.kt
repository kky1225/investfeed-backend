package com.example.investfeed.kiwoom.exception

class AccessTokenNotFound(): InvestFeedException(code = "T9999", message = "API 요청 토큰이 존재하지 않습니다.")
class KiwoomApiException(): InvestFeedException(code = "K9999", message = "API 통신 오류가 발생하였습니다.")
class InvestorDailyTradeException(): InvestFeedException(code = "K0001", message = "투자자별 일별 매매 조회에 실패하였습니다.")
class InvestorOrganizeTradeException(): InvestFeedException(code = "K0002", message = "종목별 투자자 기관별 합계 요청 조회에 실패하였습니다.")
class InvestorTradeRankException(): InvestFeedException(code = "K1002", message = "기관/외국인 매수 상위 순위 조회에 실패하였습니다.")