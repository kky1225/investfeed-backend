package com.example.investfeed.domain.holding.exception

import com.example.investfeed.common.exception.InvestFeedException

class BrokerNotFoundException : InvestFeedException(code = "BROKER_4040", message = "증권사를 찾을 수 없습니다.")
class BrokerHasMenuDependencyException(menuCount: Long) : InvestFeedException(
    code = "BROKER_4090",
    message = "이 증권사는 메뉴 ${menuCount}개의 의존성으로 등록되어 있어 삭제할 수 없습니다. 먼저 메뉴 의존성을 변경해주세요."
)
