package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.ExternalApiException

class SectInvestorException(): ExternalApiException(code = "SECT_0001", message = "업종별 투자자 순매수 조회에 실패하였습니다.")
class SectPriceNowException(): ExternalApiException(code = "SECT_0002", message = "업종 현재가 조회에 실패하셨습니다.")
class SectPriceException(): ExternalApiException(code = "SECT_0003", message = "업종별 주가 조회에 실패하셨습니다.")
class SectCodeListException(): ExternalApiException(code = "SECT_0004", message = "업종 코드 리스트 조회에 실패하였습니다.")
class SectIndexListException(): ExternalApiException(code = "SECT_0005", message = "전업종 지수 조회에 실패하였습니다.")
class SectIndexDailyListException(): ExternalApiException(code = "SECT_0005", message = "전업종 지수 일별 조회에 실패하였습니다.")