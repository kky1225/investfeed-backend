package com.example.investfeed.kiwoom.sect.dto.socket.req

data class SectIndexListStreamReq(
    var trnm: String, // 서비스명 REG : 등록 , REMOVE : 해지
    var grp_no: String, // 그룹번호
    var refresh: String, // 기존등록유지여부 등록(REG)시 0:기존유지안함 1:기존유지(Default) 0일경우 기존등록한 item/type은 해지, 1일경우 기존등록한 item/type 유지 해지(REMOVE)시 값 불필요
    var data: List<SectIndexListStream>? = null // 실시간 등록 리스트
)