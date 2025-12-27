package com.example.investfeed.kiwoom.sect.dto.req

data class KiwoomSectPriceReq (
    var mrkt_tp: String, // 시장구분
    var inds_cd: String, // 업종코드
    var stex_tp: String // 거래소구분
)