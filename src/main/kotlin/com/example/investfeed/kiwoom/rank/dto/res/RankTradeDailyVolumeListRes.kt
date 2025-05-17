package com.example.investfeed.kiwoom.rank.dto.res

data class RankTradeDailyVolumeListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var tdy_trde_qty_upper: List<RankTradeDailyVolumeList>? = null // 당일거래량상위
)