package com.example.investfeed.kiwoom.sect.dto.res

data class SectInvestor(
    var inds_cd: String, // 업종코드
    var inds_nm: String, // 업종명
    var cur_prc: String, // 현재가
    var pre_smbol: String, // 대비부호
    var pred_pre: String, // 전일대비
    var flu_rt: String, // 등락율
    var trde_qty: String, // 거래량
    var sc_netprps: String, // 증권순매수
    var insrnc_netprps: String, // 보험순매수
    var invtrt_netprps: String, // 투신순매수
    var bank_netprps: String, // 은행순매수
    var jnsinkm_netprps: String, // 종신금순매수
    var endw_netprps: String, // 기금순매수
    var etc_corp_netprps: String, // 기타법인순매수
    var ind_netprps: String, // 개인순매수
    var frgnr_netprps: String, // 외국인순매수
    var native_trmt_frgnr_netprps: String, // 내국인대우외국인순매수
    var natn_netprps: String, // 국가순매수
    var samo_fund_netprps: String, // 사모펀드순매수
    var orgn_netprps: String // 기관계순매수
)