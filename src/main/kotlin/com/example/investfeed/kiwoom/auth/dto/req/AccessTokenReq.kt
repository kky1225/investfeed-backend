package com.example.investfeed.kiwoom.auth.dto.req

data class AccessTokenReq (
    var grant_type: String = "client_credentials",
    var appkey: String,
    var secretkey: String
)