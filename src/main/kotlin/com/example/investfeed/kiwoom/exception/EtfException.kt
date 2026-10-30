package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class EtfTimeTrendException(): InvestFeedException(code = "ETF_0004", message = "ETF 시간대별 추이 조회에 실패하셨습니다.")
