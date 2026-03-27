package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class EtfPriceListException(): InvestFeedException(code = "ETF_0001", message = "ETF 전체 시세 조회에 실패하셨습니다.")
class EtfInfoException(): InvestFeedException(code = "ETF_0002", message = "ETF 정목 정보 조회에 실패하셨습니다.")
class EtfTradeDailyListException(): InvestFeedException(code = "ETF_0003", message = "ETF 일자별 체결 조회에 실패하셨습니다.")