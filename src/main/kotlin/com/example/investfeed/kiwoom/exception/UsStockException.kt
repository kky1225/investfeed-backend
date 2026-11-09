package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.ExternalApiException

class UsStockInfoException(): ExternalApiException(code = "US_STOCK_0001", message = "미국 주식 현재가 조회에 실패하셨습니다.")
class UsStockInfoListException(): ExternalApiException(code = "US_STOCK_0002", message = "미국 주식 종목 리스트 조회에 실패하셨습니다.")
class UsStockChartException(): ExternalApiException(code = "US_STOCK_0003", message = "미국 주식 차트 조회에 실패하셨습니다.")
