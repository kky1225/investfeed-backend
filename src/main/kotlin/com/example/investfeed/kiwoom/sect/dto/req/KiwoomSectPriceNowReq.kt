package com.example.investfeed.kiwoom.sect.dto.req

data class KiwoomSectPriceNowReq (
    var mrkt_tp: String, // 시장구분
    var inds_cd: String, // 업종코드
)