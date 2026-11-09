package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.ExternalApiException

class ShortSellingException(): ExternalApiException(code = "SHORT_SELLING_0001", message = "공매도 추이 조회에 실패하셨습니다.")