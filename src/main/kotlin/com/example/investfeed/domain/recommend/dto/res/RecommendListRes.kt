package com.example.investfeed.domain.recommend.dto.res

data class RecommendListRes(
    var recommendList: List<RecommendListItem>? = null,
    var avoidList: List<RecommendListItem>? = null
)