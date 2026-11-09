package com.example.investfeed.domain.marketindex.exception

import com.example.investfeed.common.exception.ExternalApiException

class MarketIndexApiException : ExternalApiException(code = "MARKET_INDEX_9999", message = "주요 시장 API 통신 오류가 발생하였습니다.")
class MarketIndexResponseException : ExternalApiException(code = "MARKET_INDEX_0001", message = "주요 시장 응답 파싱에 실패하였습니다.")
