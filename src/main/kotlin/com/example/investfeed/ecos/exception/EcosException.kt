package com.example.investfeed.ecos.exception

import com.example.investfeed.common.exception.ExternalApiException

class EcosApiException : ExternalApiException(code = "ECOS_9999", message = "ECOS API 통신 오류가 발생하였습니다.")
class EcosStatisticsException : ExternalApiException(code = "ECOS_0001", message = "ECOS 통계 조회에 실패하였습니다.")
