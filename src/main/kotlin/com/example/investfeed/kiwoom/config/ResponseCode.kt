package com.example.investfeed.kiwoom.config

enum class ResponseCode(
    val code: String = "0000",
    val message: String
) {
    DASHBOARD(message = "대시보드 조회에 성공하셨습니다."),

    TIME_NOW(message = "현재 서버 시간 조회에 성공하셨습니다."),

    SECT_CODE_LIST(message = "업종 코드 리스트 조회에 성공하셨습니다."),
    SECT_LIST(message = "전업종 지수 목록 조회에 성공하셨습니다."),
    SECT_DETAIL(message = "업종별 주식 목록 조회에 성공하셨습니다."),

    STOCK_LIST(message = "주식 목록 조회에 성공하셨습니다."),
    STOCK_DETAIL(message = "주식 상세 조회에 성공하셨습니다."),
    STOCK_DETAIL_WS(message = "실시간 주식 상세 조회에 성공하셨습니다."),

    INVESTOR_TRADE_DAILY(message = "투자자별 일별 매매 조회에 성공하셨습니다."),
    INVESTOR_TRADE_ORGANIZE(message = "종목별 투자자 기관별 합계 요청 조회에 성공하셨습니다."),
    INVESTOR_TRADE_RANK_LIST(message = "기관/외국인 매수 상위 순위 조회에 성공하셨습니다."),

    RANK_TRADE_VOLUME_LIST(message = "거래량 급증 순위 조회에 성공하셨습니다."),
    RANK_TRADE_DAILY_VOLUME_LIST(message = "당일 거래량 상위 순위 조회에 성공하셨습니다."),

    THEME_GROUP_LIST(message = "테마 그룹별 조회에 성공하셨습니다."),
    THEME_GROUP_STOCK_LIST(message = "테마 구성 종목 조회에 성공하셨습니다."),

    SECT_CHART_MINUTE_LIST(message = "업종 분봉 차트 조회에 성공하셨습니다."),

    ETF_PRICE_LIST(message = "ETF 전체 시세 조회에 성공하셨습니다."),
    ETF_INFO(message = "ETF 정목 정보 조회에 성공하셨습니다."),
    ETF_TRADE_DAILY_LIST(message = "ETF 일자별 체결 조회에 성공하셨습니다."),

    INDEX_LIST(message = "지수 목록 조회에 성공하셨습니다."),
    INDEX_WS_LIST(message = "지수 목록 소켓 조회에 성공하셨습니다."),
    INDEX_DETAIL(message = "지수 상세 조회에 성공하셨습니다."),
    INDEX_WS_DETAIL(message = "지수 상세 소켓 조회에 성공하셨습니다."),

    COMMODITY_LIST(message = "원자재 목록 조회에 성공하셨습니다."),
    COMMODITY_LIST_REALTIME(message = "원자재 목록 실시간 조회에 성공하셨습니다."),
    COMMODITY_DETAIL(message = "원자재 상세 조회에 성공하셨습니다."),
    COMMODITY_DETAIL_REALTIME(message = "원자재 상세 실시간 조회에 성공하셨습니다."),
}