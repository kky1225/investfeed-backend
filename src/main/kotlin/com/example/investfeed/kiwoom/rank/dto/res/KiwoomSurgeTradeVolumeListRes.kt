package com.example.investfeed.kiwoom.rank.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomSurgeTradeVolumeListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var trde_qty_sdnin: List<KiwoomSurgeTradeVolumeRes>? = null // 거래량급증
): KiwoomRes(return_code, return_msg)