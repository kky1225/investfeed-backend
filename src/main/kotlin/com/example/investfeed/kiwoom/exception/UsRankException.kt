package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class UsStockTradeValueListException(): InvestFeedException(code = "US_RANK_0001", message = "미국 주식 거래대금 상위 조회에 실패하셨습니다.")
class UsStockTradeVolumeListException(): InvestFeedException(code = "US_RANK_0002", message = "미국 주식 거래량 상위 조회에 실패하셨습니다.")
class UsSurgeTradeVolumeListException(): InvestFeedException(code = "US_RANK_0003", message = "미국 주식 거래량 급증 조회에 실패하셨습니다.")
