package com.example.investfeed.kiwoom.stock.entity.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockTradeVolumeListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var tdy_trde_qty_upper: List<KiwoomStockTradeVolumeRes>? = null // 당일거래량상위
): KiwoomRes(return_code, return_msg)