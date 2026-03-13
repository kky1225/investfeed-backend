package com.example.investfeed.kiwoom.exception

class AuthException(
    val code: String,
    override val message: String
) : RuntimeException(message)
