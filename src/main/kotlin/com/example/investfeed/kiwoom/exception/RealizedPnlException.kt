package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.ExternalApiException

class RealizedPnlException : ExternalApiException(code = "REALIZED_PNL_0001", message = "실현손익 조회에 실패하였습니다.")
