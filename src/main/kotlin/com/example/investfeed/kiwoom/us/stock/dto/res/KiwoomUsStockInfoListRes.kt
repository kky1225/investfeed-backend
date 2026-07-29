package com.example.investfeed.kiwoom.us.stock.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomUsStockInfoListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var list: List<KiwoomUsStockInfoListItem>? = null // 결과리스트
): KiwoomRes(return_code, return_msg)
