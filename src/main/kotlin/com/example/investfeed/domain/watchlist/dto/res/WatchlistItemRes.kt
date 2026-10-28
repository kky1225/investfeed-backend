package com.example.investfeed.domain.watchlist.dto.res

data class WatchlistItemRes(
    val id: Long,
    val stkCd: String,
    val stkNm: String,
    val stexTp: String? = null, // NULL: 국내, ND/NY/NA: 미국 거래소
    var curPrc: String? = null,
    var fluRt: String? = null,
    var preSig: String? = null,
    var mrktNm: String? = null, // 국내 시장명
    var sessionLabel: String? = null, // 미국 데이·프리·애프터마켓 (토스 연동 회원만)
    var regularClose: Double? = null, // 장구분 등락률 기준 정규장 종가 (USD)
)
