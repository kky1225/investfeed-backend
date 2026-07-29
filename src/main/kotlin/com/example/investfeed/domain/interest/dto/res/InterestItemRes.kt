package com.example.investfeed.domain.interest.dto.res

data class InterestItemRes(
    val id: Long,
    val stkCd: String,
    val stkNm: String,
    val stexTp: String? = null, // NULL: 국내, ND/NY/NA: 미국 거래소
    var curPrc: String? = null,
    var fluRt: String? = null,
    var preSig: String? = null,
)
