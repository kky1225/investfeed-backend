package com.example.investfeed.kiwoom.exception

data class ApiResponse<T> (
    var code: String,
    var message: String,
    var result: T
)