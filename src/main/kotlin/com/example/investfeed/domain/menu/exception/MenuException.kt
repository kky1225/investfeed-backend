package com.example.investfeed.domain.menu.exception

import com.example.investfeed.common.exception.InvestFeedException

class MenuNotFoundException : InvestFeedException(code = "MENU_4010", message = "메뉴를 찾을 수 없습니다.")
class MenuHasChildrenException : InvestFeedException(code = "MENU_4011", message = "하위 메뉴가 존재합니다. 하위 메뉴를 먼저 삭제해주세요.")
