package com.example.investfeed.domain.rank.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class RankListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var rankList: List<RankListItem>? = null,
): KiwoomRes(return_code, return_msg)