package com.example.investfeed.kiwoom.price.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockSinglePriceRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var bid_req_base_tm: String? = null, // 호가잔량기준시간
    var ovt_sigpric_sel_bid_jub_pre_5: String? = null, // 시간외단일가_매도호가직전대비5
    var ovt_sigpric_sel_bid_jub_pre_4: String? = null, // 시간외단일가_매도호가직전대비4
    var ovt_sigpric_sel_bid_jub_pre_3: String? = null, // 시간외단일가_매도호가직전대비3
    var ovt_sigpric_sel_bid_jub_pre_2: String? = null, // 시간외단일가_매도호가직전대비2
    var ovt_sigpric_sel_bid_jub_pre_1: String? = null, // 시간외단일가_매도호가직전대비1
    var ovt_sigpric_sel_bid_qty_5: String? = null, // 시간외단일가_매도호가수량5
    var ovt_sigpric_sel_bid_qty_4: String? = null, // 시간외단일가_매도호가수량4
    var ovt_sigpric_sel_bid_qty_3: String? = null, // 시간외단일가_매도호가수량3
    var ovt_sigpric_sel_bid_qty_2: String? = null, // 시간외단일가_매도호가수량2
    var ovt_sigpric_sel_bid_qty_1: String? = null, // 시간외단일가_매도호가수량1
    var ovt_sigpric_sel_bid_5: String? = null, // 시간외단일가_매도호가5
    var ovt_sigpric_sel_bid_4: String? = null, // 시간외단일가_매도호가4
    var ovt_sigpric_sel_bid_3: String? = null, // 시간외단일가_매도호가3
    var ovt_sigpric_sel_bid_2: String? = null, // 시간외단일가_매도호가2
    var ovt_sigpric_sel_bid_1: String? = null, // 시간외단일가_매도호가1
    var ovt_sigpric_buy_bid_1: String? = null, // 시간외단일가_매수호가1
    var ovt_sigpric_buy_bid_2: String? = null, // 시간외단일가_매수호가2
    var ovt_sigpric_buy_bid_3: String? = null, // 시간외단일가_매수호가3
    var ovt_sigpric_buy_bid_4: String? = null, // 시간외단일가_매수호가4
    var ovt_sigpric_buy_bid_5: String? = null, // 시간외단일가_매수호가5
    var ovt_sigpric_buy_bid_qty_1: String? = null, // 시간외단일가_매수호가수량1
    var ovt_sigpric_buy_bid_qty_2: String? = null, // 시간외단일가_매수호가수량2
    var ovt_sigpric_buy_bid_qty_3: String? = null, // 시간외단일가_매수호가수량3
    var ovt_sigpric_buy_bid_qty_4: String? = null, // 시간외단일가_매수호가수량4
    var ovt_sigpric_buy_bid_qty_5: String? = null, // 시간외단일가_매수호가수량5
    var ovt_sigpric_buy_bid_jub_pre_1: String? = null, // 시간외단일가_매수호가직전대비1
    var ovt_sigpric_buy_bid_jub_pre_2: String? = null, // 시간외단일가_매수호가직전대비2
    var ovt_sigpric_buy_bid_jub_pre_3: String? = null, // 시간외단일가_매수호가직전대비3
    var ovt_sigpric_buy_bid_jub_pre_4: String? = null, // 시간외단일가_매수호가직전대비4
    var ovt_sigpric_buy_bid_jub_pre_5: String? = null, // 시간외단일가_매수호가직전대비5
    var ovt_sigpric_sel_bid_tot_req: String? = null, // 시간외단일가_매도호가총잔량
    var ovt_sigpric_buy_bid_tot_req: String? = null, // 시간외단일가_매수호가총잔량
    var sel_bid_tot_req_jub_pre: String? = null, // 매도호가총잔량직전대비
    var sel_bid_tot_req: String? = null, // 매도호가총잔량
    var buy_bid_tot_req: String? = null, // 매수호가총잔량
    var buy_bid_tot_req_jub_pre: String? = null, // 매수호가총잔량직전대비
    var ovt_sel_bid_tot_req_jub_pre: String? = null, // 시간외매도호가총잔량직전대비
    var ovt_sel_bid_tot_req: String? = null, // 시간외매도호가총잔량
    var ovt_buy_bid_tot_req: String? = null, // 시간외매수호가총잔량
    var ovt_buy_bid_tot_req_jub_pre: String? = null, // 시간외매수호가총잔량직전대비
    var ovt_sigpric_cur_prc: String? = null, // 시간외단일가_현재가
    var ovt_sigpric_pred_pre_sig: String? = null, // 시간외단일가_전일대비기호
    var ovt_sigpric_pred_pre: String? = null, // 시간외단일가_전일대비
    var ovt_sigpric_flu_rt: String? = null, // 시간외단일가_등락률
    var ovt_sigpric_acc_trde_qty: String? = null // 시간외단일가_누적거래량
): KiwoomRes(return_code, return_msg)