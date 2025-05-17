package com.example.investfeed.kiwoom.rank.dto.res

data class RankTradeVolumeListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var trde_qty_sdnin: List<RankTradeVolumeList>? = null // 거래량급증
)