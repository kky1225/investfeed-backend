package com.example.investfeed.kiwoom.exception

class ShortSellingException(): InvestFeedException(code = "SHORT_SELLING_0001", message = "공매도 추이 조회에 실패하셨습니다.")