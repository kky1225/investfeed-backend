package com.example.investfeed.domain.recommend.dto.res

import java.time.LocalDate

data class RecommendListRes(
    var recommendList: List<RecommendListItem>? = null,
    var avoidList: List<RecommendListItem>? = null,
    var holdList: List<RecommendListItem>? = null,
    var pickedDate: LocalDate? = null,   // 마지막으로 산출한 추천의 기준일
    var stale: Boolean = false,          // 직전 거래일 22:00 이후 추천 배치가 성공하지 않음 (지난 추천)
)
