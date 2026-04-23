package com.example.investfeed.domain.dividend.exception

import com.example.investfeed.common.exception.InvestFeedException

class StockDividendApiException : InvestFeedException(code = "DIVIDEND_9001", message = "주식 배당 API 통신 오류가 발생하였습니다.")
class StockDividendResponseException : InvestFeedException(code = "DIVIDEND_0001", message = "주식 배당 조회에 실패하였습니다.")
class EtfDividendApiException : InvestFeedException(code = "DIVIDEND_9002", message = "ETF 분배금 API 통신 오류가 발생하였습니다.")
class EtfDividendResponseException : InvestFeedException(code = "DIVIDEND_0002", message = "ETF 분배금 조회에 실패하였습니다.")
