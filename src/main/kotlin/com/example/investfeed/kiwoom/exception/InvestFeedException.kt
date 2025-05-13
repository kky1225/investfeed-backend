package com.example.investfeed.kiwoom.exception

open class InvestFeedException(
    val code: String,
    override val message: String
): RuntimeException(message)