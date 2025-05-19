package com.example.investfeed.kiwoom.exception

class ThemeGroupListException(): InvestFeedException(code = "THEME_0001", message = "테마 그룹별 조회에 실패하셨습니다.")
class ThemeGroupStockListException(): InvestFeedException(code = "THEME_0002", message = "테마 구성 종목 조회에 실패하셨습니다.")