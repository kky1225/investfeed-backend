package com.example.investfeed.kiwoom.sect.dto.rest.req

data class SectPriceNowReq (
    var mrkt_tp: String, // 시장구분
    var inds_cd: String, // 업종코드
)