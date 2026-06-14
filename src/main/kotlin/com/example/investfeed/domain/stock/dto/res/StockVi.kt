package com.example.investfeed.domain.stock.dto.res

data class StockVi(
    var motnPric: String? = null,          // 발동가격
    var motnTime: String? = null,          // 발동시각 (HHmmss)
    var relisTime: String? = null,         // 해제시각 (HHmmss)
    var viType: String? = null,            // 적용구분 (정적/동적/동적+정적)
    var dynmDisptyRt: String? = null,      // 동적괴리율
    var staticDisptyRt: String? = null,    // 정적괴리율
    var openPricPreFluRt: String? = null,  // 시가대비등락률
    var vimotnCnt: String? = null,         // 발동횟수
    var direction: String? = null,         // 발동방향 (상승/하락)
    var active: Boolean = false,           // 현재 발동중 여부 (해제시각 미존재)
)
