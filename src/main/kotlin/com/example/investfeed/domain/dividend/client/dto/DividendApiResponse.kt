package com.example.investfeed.domain.dividend.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DividendApiResponse(
    val response: DividendApiResponseBody? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DividendApiResponseBody(
    val header: DividendApiHeader? = null,
    val body: DividendApiBody? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DividendApiHeader(
    val resultCode: String? = null,
    val resultMsg: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DividendApiBody(
    val numOfRows: Int? = null,
    val pageNo: Int? = null,
    val totalCount: Int? = null,
    val items: DividendApiItems? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DividendApiItems(
    val item: List<DividendApiItem>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DividendApiItem(
    val basDt: String? = null,
    val crno: String? = null,
    val isinCd: String? = null,
    val stckIssuCmpyNm: String? = null,
    val isinCdNm: String? = null,
    val scrsItmsKcd: String? = null,
    val scrsItmsKcdNm: String? = null,
    val stckParPrc: String? = null,
    val stckStacMd: String? = null,
    val dvdnBasDt: String? = null,
    val cashDvdnPayDt: String? = null,
    val stckHndvDt: String? = null,
    val stckDvdnRcd: String? = null,
    val stckDvdnRcdNm: String? = null,
    val stckGenrDvdnAmt: String? = null,
    val stckGrdnDvdnAmt: String? = null,
    val stckGenrCashDvdnRt: String? = null,
    val stckGenrDvdnRt: String? = null,
    val cashGrdnDvdnRt: String? = null,
    val stckGrdnDvdnRt: String? = null,
)
