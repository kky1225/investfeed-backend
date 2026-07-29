package com.example.investfeed.kiwoom.us.rank.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomUsSurgeTradeVolumeListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var result_list: List<KiwoomUsSurgeTradeVolumeRes>? = null // 거래량 급등락
): KiwoomRes(return_code, return_msg)
