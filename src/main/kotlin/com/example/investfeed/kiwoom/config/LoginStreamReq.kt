package com.example.investfeed.kiwoom.config

data class LoginStreamReq(
    val trnm: String = "LOGIN",
    var token: String
)