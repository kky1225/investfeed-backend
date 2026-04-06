package com.example.investfeed.kiwoom.etf.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class EtfPriceListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var etfall_mrpr: List<EtfPriceList>? = null // ETF 전체시세
): KiwoomRes(return_code, return_msg)