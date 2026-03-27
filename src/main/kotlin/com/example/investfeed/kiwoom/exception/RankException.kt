package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class InvestorTradeDailyException() : InvestFeedException(code = "RANK_0001", message = "장중 투자자별 매매 상위 조회에 실패하셨습니다.")
class InvestorTradeException() : InvestFeedException(code = "RANK_0001", message = "외국인/기관 매매 상위 조회에 실패하셨습니다.")