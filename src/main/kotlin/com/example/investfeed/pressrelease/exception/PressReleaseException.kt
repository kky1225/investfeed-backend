package com.example.investfeed.pressrelease.exception

import com.example.investfeed.common.exception.InvestFeedException

class PressReleaseApiException : InvestFeedException(code = "PRESS_RELEASE_9999", message = "보도자료 RSS 통신 오류가 발생하였습니다.")
class PressReleaseResponseException : InvestFeedException(code = "PRESS_RELEASE_0001", message = "보도자료 RSS 조회에 실패하였습니다.")
