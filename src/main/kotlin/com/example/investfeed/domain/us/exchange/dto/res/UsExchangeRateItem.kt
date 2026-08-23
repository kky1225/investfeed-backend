package com.example.investfeed.domain.us.exchange.dto.res

data class UsExchangeRateItem(
    val exchTp: String?, // 환전구분 1:원화(KRW)->달러(USD), 2:달러(USD)->원화(KRW)
    val sellAplcExrt: String?, // 매도적용환율
    val buyAplcExrt: String?, // 매수적용환율
    val aplcExrt: String?, // 적용환율 (실제 환전에 적용되는 환율)
    val exrtTpNm: String?, // 환율구분명 (예: 고시환율)
    val spclBfExrt: String?, // 우대율 적용 전 환율
    val exrtSpclRt: String?, // 환율우대율
)
