package com.example.investfeed.domain.rank.dto.req

data class RankListReq(
    var type: String, // 0: 거래대금, 1: 거래량, 2: 급등
)