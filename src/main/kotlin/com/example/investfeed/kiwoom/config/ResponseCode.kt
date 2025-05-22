package com.example.investfeed.kiwoom.config

enum class ResponseCode(
    val code: String = "0000",
    val message: String
) {
    SECT_PRICE(message = "업종별 주가 조회에 성공하셨습니다."),
    SECT_INVESTOR(message = "업종별 투자자 순매수 조회에 성공하셨습니다."),
    SECT_PRICE_NOW(message = "업종 현재가 조회에 성공하셨습니다."),
    SECT_CODE_LIST(message = "업종 코드 리스트 조회에 성공하셨습니다."),
    SECT_INDEX_LIST(message = "전업종 지수 조회에 성공하셨습니다."),

    STOCK_INFO_LIST(message = "정목 정보 리스트 조회에 성공하셨습니다."),
    STOCK_INFO(message = "주식 기본 정보 조회에 성공하셨습니다."),
    STOCK_TRADE_DAILY_LIST(message = "일별 거래 상세 조회에 성공하셨습니다."),
    STOCK_JUMP_LIST(message = "가격 급등락 조회에 성공하셨습니다."),
    STOCK_SINGLE_PRICE_LIST(message = "시간외단일가 조회에 성공하셨습니다."),

    INVESTOR_TRADE_DAILY(message = "투자자별 일별 매매 조회에 성공하셨습니다."),
    INVESTOR_TRADE_ORGANIZE(message = "종목별 투자자 기관별 합계 요청 조회에 성공하셨습니다."),
    INVESTOR_TRADE_RANK_LIST(message = "기관/외국인 매수 상위 순위 조회에 성공하셨습니다."),

    RANK_TRADE_VOLUME_LIST(message = "거래량 급증 순위 조회에 성공하셨습니다."),
    RANK_TRADE_DAILY_VOLUME_LIST(message = "당일 거래량 상위 순위 조회에 성공하셨습니다."),

    THEME_GROUP_LIST(message = "테마 그룹별 조회에 성공하셨습니다."),
    THEME_GROUP_STOCK_LIST(message = "테마 구성 종목 조회에 성공하셨습니다."),

    CHART_TICK_LIST(message = "주식 틱 차트 조회에 성공하셨습니다."),
    CHART_MINUTE_LIST(message = "주식 분봉 차트 조회에 성공하셨습니다."),
    CHART_DAY_LIST(message = "주식 일봉 차트 조회에 성공하셨습니다."),
    CHART_WEEK_LIST(message = "주식 주봉 차트 조회에 성공하셨습니다."),
    CHART_MONTH_LIST(message = "주식 월봉 차트 조회에 성공하셨습니다."),
    CHART_YEAR_LIST(message = "주식 년봉 차트 조회에 성공하셨습니다."),

    ETF_PRICE_LIST(message = "ETF 전체 시세 조회에 성공하셨습니다."),
    ETF_INFO(message = "ETF 정목 정보 조회에 성공하셨습니다."),
    ETF_TRADE_DAILY_LIST(message = "ETF 일자별 체결 조회에 성공하셨습니다."),
}