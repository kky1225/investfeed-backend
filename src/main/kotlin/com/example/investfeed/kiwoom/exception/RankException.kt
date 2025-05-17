package com.example.investfeed.kiwoom.exception

class RankTradeVolumeListException(): InvestFeedException(code = "RANK_0001", message = "거래량 급증 순위 조회에 실패하셨습니다.")
class RankTradeDailyVolumeListException(): InvestFeedException(code = "RANK_0002", message = "당일 거래량 상위 순위 조회에 실패하셨습니다.")