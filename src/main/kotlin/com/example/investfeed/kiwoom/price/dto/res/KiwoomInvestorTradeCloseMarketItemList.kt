package com.example.investfeed.kiwoom.price.dto.res

data class KiwoomInvestorTradeCloseMarketItemList(
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var cur_prc: String? = null, // 현재가
    var pre_sig: String? = null, // 대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락률
    var trde_qty: String? = null, // 거래량
    var ind_invsr: String? = null, // 개인투자자
    var frgnr_invsr: String? = null, // 외국인투자자
    var orgn: String? = null, // 기관계
    var fnnc_invt: String? = null, // 금융투자
    var insrnc: String? = null, // 보험
    var invtrt: String? = null, // 투신
    var etc_fnnc: String? = null, // 기타금융
    var bank: String? = null, // 은행
    var penfnd_etc: String? = null, // 연기금등
    var samo_fund: String? = null, // 사모펀드
    var natn: String? = null, // 국가
    var etc_corp: String? = null, // 기타법인
)