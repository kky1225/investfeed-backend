package com.example.investfeed.domain.dividend.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EtfDividendApiResponse(
    val status: String? = null,
    val stock_code: String? = null,
    val dividends: List<EtfDividendApiItem>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EtfDividendApiItem(
    val date: String? = null,
    val year: Int? = null,
    val month: Int? = null,
    val amount: Int? = null,
)
