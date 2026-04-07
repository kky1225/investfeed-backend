package com.example.investfeed.kiwoom.stock.dto.req

data class KiwoomNewHighLowReq(
    val mrkt_tp: String = "000",
    val ntl_tp: String,
    val high_low_close_tp: String = "1",
    val stk_cnd: String = "0",
    val trde_qty_tp: String = "00000",
    val crd_cnd: String = "0",
    val updown_incls: String = "1",
    val dt: String = "250",
    val stex_tp: String = "3",
)
