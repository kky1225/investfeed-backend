package com.example.investfeed.kiwoom.holding.dto.res

data class KiwoomHoldingRes(
    var return_code: Int,
    var return_msg: String,
    var tot_pur_amt: String? = null, // 총매입금액
    var tot_evlt_amt: String? = null, // 총평가금액
    var tot_evlt_pl: String? = null, // 총평가손익금액
    var tot_prft_rt: String? = null, // 총수익률(%)
    var prsm_dpst_aset_amt: String? = null, // 추정예탁자산
    var acnt_evlt_remn_indv_tot: List<KiwoomHoldingStock>? = null // 계좌평가잔고개별합산
)

data class KiwoomHoldingStock(
    var stk_cd: String? = null, // 종목번호
    var stk_nm: String? = null, // 종목명
    var evltv_prft: String? = null, // 평가손익
    var prft_rt: String? = null, // 수익률(%)
    var pur_pric: String? = null, // 매입가
    var pred_close_pric: String? = null, // 전일종가
    var rmnd_qty: String? = null, // 보유수량
    var trde_able_qty: String? = null, // 매매가능수량
    var cur_prc: String? = null, // 현재가
    var pur_amt: String? = null, // 매입금액
    var pur_cmsn: String? = null, // 매입수수료
    var evlt_amt: String? = null, // 평가금액
    var sell_cmsn: String? = null, // 평가수수료
    var tax: String? = null, // 세금
    var sum_cmsn: String? = null, // 수수료합 (매입수수료 + 평가수수료)
    var poss_rt: String? = null, // 보유비중(%)
)
