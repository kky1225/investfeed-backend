package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.ExternalApiException

class ThemeGroupListException(): ExternalApiException(code = "THEME_0001", message = "테마 그룹별 조회에 실패하셨습니다.")
class ThemeGroupStockListException(): ExternalApiException(code = "THEME_0002", message = "테마 구성 종목 조회에 실패하셨습니다.")