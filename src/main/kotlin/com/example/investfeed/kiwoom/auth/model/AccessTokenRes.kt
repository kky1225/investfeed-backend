package com.example.investfeed.kiwoom.auth.model

data class AccessTokenRes (
    var expires_dt: String,
    var token_type: String,
    var token: String
)