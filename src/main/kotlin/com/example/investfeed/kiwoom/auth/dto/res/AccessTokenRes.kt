package com.example.investfeed.kiwoom.auth.dto.res

data class AccessTokenRes (
    var expires_dt: String,
    var token_type: String,
    var token: String
)