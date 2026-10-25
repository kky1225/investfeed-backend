package com.example.investfeed.domain.sect.dto.res

data class SectListItem (
    var stkCd: String? = null,
    var stkNm: String? = null,
    var preSig: String? = null,
    var fluRt: String? = null,
    var curPrc: String? = null,
    var trdeQty: String? = null,
    var trdePrica: String? = null,
    var rising: String? = null, // 상승 종목수
    var stdns: String? = null, // 보합 종목수
    var fall: String? = null, // 하락 종목수
    var floStkNum: String? = null, // 상장 종목수
)