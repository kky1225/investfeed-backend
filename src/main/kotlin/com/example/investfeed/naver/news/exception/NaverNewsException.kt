package com.example.investfeed.naver.news.exception

import com.example.investfeed.common.exception.InvestFeedException

class NaverNewsApiException : InvestFeedException(code = "NAVER_NEWS_9999", message = "네이버 뉴스 API 통신 오류가 발생하였습니다.")
class NaverNewsSearchException : InvestFeedException(code = "NAVER_NEWS_0001", message = "네이버 뉴스 검색에 실패하였습니다.")
