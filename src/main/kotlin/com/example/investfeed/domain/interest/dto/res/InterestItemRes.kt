package com.example.investfeed.domain.interest.dto.res

data class InterestItemRes(
    val id: Long,
    val stkCd: String,
    val stkNm: String,
    var curPrc: String? = null,
    var fluRt: String? = null,
    var preSig: String? = null,
)
