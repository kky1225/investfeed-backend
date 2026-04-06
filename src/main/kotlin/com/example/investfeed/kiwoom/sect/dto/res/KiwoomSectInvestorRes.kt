package com.example.investfeed.kiwoom.sect.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomSectInvestorRes(
    override var return_code: Int, // 결과 코드
    override var return_msg: String, // 결과 메세지
    var inds_netprps: List<KiwoomSectInvestor>? = null // 업종별순매수
): KiwoomRes(return_code, return_msg)