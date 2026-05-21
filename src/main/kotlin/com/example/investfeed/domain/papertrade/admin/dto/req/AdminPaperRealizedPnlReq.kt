package com.example.investfeed.domain.papertrade.admin.dto.req

data class AdminPaperRealizedPnlReq(
    val viewMode: String = "monthly",
    val year: Int? = null,
    val month: Int? = null,
)
