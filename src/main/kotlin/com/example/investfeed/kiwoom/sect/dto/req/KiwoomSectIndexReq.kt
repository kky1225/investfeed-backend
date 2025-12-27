package com.example.investfeed.kiwoom.sect.dto.req

import com.example.investfeed.kiwoom.sect.dto.socket.req.SectIndexListStream

data class KiwoomSectIndexReq(
    var inds_cd: String, // 업종코드 001:종합(KOSPI), 002:대형주, 003:중형주, 004:소형주 101:종합(KOSDAQ), 201:KOSPI200, 302:KOSTAR, 701: KRX100 나머지 ※ 업종코드 참고
    val trnm: String, // 서비스명 REG : 등록 , REMOVE : 해지
    val grp_no: String, // 그룹번호
    val refresh: String, // 기존등록유지여부 등록(REG)시 0:기존유지안함 1:기존유지(Default) 0일경우 기존등록한 item/type은 해지, 1일경우 기존등록한 item/type 유지 해지(REMOVE)시 값 불필요
    val data: List<SectIndexListStream>? = null // 실시간 등록 리스트
)