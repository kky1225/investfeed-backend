package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class UsSectPerformanceListException(): InvestFeedException(code = "US_SECT_0001", message = "미국 업종별 기간별 수익률 조회에 실패하셨습니다.")
class UsSectStockListException(): InvestFeedException(code = "US_SECT_0002", message = "미국 업종별 주식 목록 조회에 실패하셨습니다.")
