package com.example.investfeed.common.exception

open class InvestFeedException(
    val code: String,
    override val message: String
) : RuntimeException(message)
