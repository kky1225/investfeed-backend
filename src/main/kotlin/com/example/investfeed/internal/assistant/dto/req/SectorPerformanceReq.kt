package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.internal.assistant.tool.SectorToolService

data class SectorPerformanceReq(
    val market: SectorToolService.SectorMarket,           // KR(코스피·코스닥 둘 다)/KOSPI/KOSDAQ/US 필수 — 국내·미국이 없으면 비서가 되묻는다
    val sector: String? = null,                           // 업종명 (전기/전자, 반도체, 코스닥 화학). 있으면 시장별 소속 종목
    val multiDay: Boolean? = null,                        // 기간 수익률("한 달", "올해")을 물었으면 true → 당일만 제공 안내 문장
    val sort: SectorToolService.SectorSort? = null,       // 미지정 = 등락률 높은 순
    val date: String? = null,                             // 당일(개장 전·휴장일이면 직전 거래일)만. 과거 날짜는 안내 문장
)
