package com.example.investfeed.kiwoom.exception

import com.example.investfeed.common.exception.InvestFeedException

class BuyOrderException() : InvestFeedException(code = "ORDER_0001", message = "매수 주문에 실패하였습니다.")
class SellOrderException() : InvestFeedException(code = "ORDER_0002", message = "매도 주문에 실패하였습니다.")
class CancelOrderException() : InvestFeedException(code = "ORDER_0003", message = "주문 취소에 실패하였습니다.")
class PendingOrderException() : InvestFeedException(code = "ORDER_0004", message = "미체결 조회에 실패하였습니다.")
class TradeFillsException() : InvestFeedException(code = "ORDER_0005", message = "체결내역 조회에 실패하였습니다.")