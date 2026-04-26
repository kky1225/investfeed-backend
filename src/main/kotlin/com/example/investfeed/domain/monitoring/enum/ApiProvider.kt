package com.example.investfeed.domain.monitoring.enum

/**
 * 외부 API provider 카탈로그.
 *
 * - dailyLimit: 명시적 일간 한도 (없으면 null). 운영 도구상 진행률 표시 용도.
 * - 한도가 없어도 7일 호출 추세는 항상 표시 — IP 차단/재시도 폭주/무한 루프 등 호출 폭증 조기 감지 목적.
 */
enum class ApiProvider(val label: String, val dailyLimit: Long?) {
    KIWOOM("키움증권", null),
    UPBIT("Upbit", null),
    FRED("FRED", null),
    ECOS("ECOS (한국은행)", null),                          // 공식 문서상 일일 한도 미공개 (rate limit 위주로 추정)
    FEAR_GREED("Fear & Greed", null),
    NAVER_NEWS("Naver 뉴스 검색", 25_000),                // 일반 등급 25,000건/일 (Open API)
    NAVER_CRAWL("Naver (크롤링)", null),                  // 비공식 크롤링, 공식 한도 없음 (남용 시 IP 차단)
    PUBLIC_DATA_HOLIDAY("공공데이터 (휴일)", 10_000),       // 공공데이터포털 SpcdeInfoService /getRestDeInfo 10,000건/일
    PUBLIC_DATA_DIVIDEND("공공데이터 (주식배당)", 10_000),  // 공공데이터포털 GetStocDiviInfoService 10,000건/일
    SEARCH_ETF("Search ETF", null),                       // 비공식 사이트, 알려진 한도 없음
}
