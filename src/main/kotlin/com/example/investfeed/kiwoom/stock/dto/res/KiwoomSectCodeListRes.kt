package com.example.investfeed.kiwoom.stock.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomSectCodeListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var list: List<KiwoomSectCodeList> // 업종코드리스트
): KiwoomRes(return_code, return_msg)