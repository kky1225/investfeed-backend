package com.example.investfeed.common.exception

data class ApiResponse<T> (
    var code: String,
    var message: String,
    var result: T
)
