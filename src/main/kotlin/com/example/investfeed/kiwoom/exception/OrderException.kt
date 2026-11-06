package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class BuyOrderException() : InvestFeedException(code = "ORDER_0001", message = "매수 주문에 실패하였습니다.")
class SellOrderException() : InvestFeedException(code = "ORDER_0002", message = "매도 주문에 실패하였습니다.")
class CancelOrderException() : InvestFeedException(code = "ORDER_0003", message = "주문 취소에 실패하였습니다.")
class PendingOrderException() : InvestFeedException(code = "ORDER_0004", message = "미체결 조회에 실패하였습니다.")
class TradeFillsException() : InvestFeedException(code = "ORDER_0005", message = "체결내역 조회에 실패하였습니다.")

/** 주문 요청 후 응답을 받지 못함(시간 초과·연결 끊김 등). 키움이 접수했는지 알 수 없다 */
class OrderResultUnknownException() : InvestFeedException(code = "ORDER_0006", message = "주문 결과를 확인하지 못했습니다.")