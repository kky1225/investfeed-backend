package com.example.investfeed.domain

enum class ResponseCode(
    val code: String = "0000",
    val message: String
) {
    DASHBOARD(message = "대시보드 조회에 성공하셨습니다."),

    TIME_NOW(message = "현재 서버 시간 조회에 성공하셨습니다."),

    SECT_LIST(message = "전업종 지수 목록 조회에 성공하셨습니다."),
    SECT_LIST_STREAM(message = "실시간 전업종 지수 목록 조회에 성공하셨습니다."),
    SECT_STOCK_LIST(message = "업종별 주식 목록 조회에 성공하셨습니다."),

    STOCK_LIST(message = "주식 목록 조회에 성공하셨습니다."),
    STOCK_DETAIL(message = "주식 상세 조회에 성공하셨습니다."),
    STOCK_DETAIL_WS(message = "실시간 주식 상세 조회에 성공하셨습니다."),
    STOCK_SEARCH(message = "종목 검색에 성공하셨습니다."),

    INVESTOR_LIST(message = "투자자별 목록 조회에 성공하셨습니다."),

    INVESTOR_TRADE_DAILY(message = "투자자별 일별 매매 조회에 성공하셨습니다."),
    INVESTOR_TRADE_ORGANIZE(message = "종목별 투자자 기관별 합계 요청 조회에 성공하셨습니다."),
    INVESTOR_TRADE_RANK_LIST(message = "기관/외국인 매수 상위 순위 조회에 성공하셨습니다."),

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

    THEME_LIST(message = "테마 그룹별 조회에 성공하셨습니다."),
    THEME_STOCK_LIST(message = "테마 구성 종목 조회에 성공하셨습니다."),
    THEME_STOCK_LIST_STREAM(message = "실시간 테마 구성 종목 조회에 성공하셨습니다."),

    RECOMMEND_LIST(message = "주식 추천 목록 조회에 성공하셨습니다."),
    RECOMMEND_LIST_STREAM(message = "실시간 주식 추천 목록 조회에 성공하셨습니다."),

    AUTH_SIGNUP(message = "회원가입에 성공하셨습니다."),
    AUTH_LOGIN(message = "로그인에 성공하셨습니다."),
    AUTH_REISSUE(message = "토큰 재발급에 성공하셨습니다."),
    AUTH_LOGOUT(message = "로그아웃에 성공하셨습니다."),

    INTEREST_GROUP_LIST(message = "관심종목 그룹 목록 조회에 성공하셨습니다."),
    INTEREST_GROUP_CREATE(message = "관심종목 그룹 생성에 성공하셨습니다."),
    INTEREST_GROUP_UPDATE(message = "관심종목 그룹 수정에 성공하셨습니다."),
    INTEREST_GROUP_DELETE(message = "관심종목 그룹 삭제에 성공하셨습니다."),
    INTEREST_GROUP_REORDER(message = "관심종목 그룹 순서 변경에 성공하셨습니다."),
    INTEREST_ITEM_LIST(message = "관심종목 목록 조회에 성공하셨습니다."),
    INTEREST_ITEM_ADD(message = "관심종목 추가에 성공하셨습니다."),
    INTEREST_ITEM_DELETE(message = "관심종목 삭제에 성공하셨습니다."),
    INTEREST_ITEM_REORDER(message = "관심종목 순서 변경에 성공하셨습니다."),
    INTEREST_ITEM_STREAM(message = "실시간 관심종목 조회에 성공하셨습니다."),

    CRYPTO_LIST(message = "암호화폐 목록 조회에 성공하셨습니다."),
    CRYPTO_LIST_STREAM(message = "실시간 암호화폐 목록 조회에 성공하셨습니다."),
    CRYPTO_DETAIL(message = "암호화폐 상세 조회에 성공하셨습니다."),
    CRYPTO_FEAR_GREED(message = "공포탐욕지수 조회에 성공하셨습니다."),
    CRYPTO_SEARCH(message = "암호화폐 검색에 성공하셨습니다."),

    CRYPTO_INTEREST_GROUP_LIST(message = "암호화폐 관심종목 그룹 목록 조회에 성공하셨습니다."),
    CRYPTO_INTEREST_GROUP_CREATE(message = "암호화폐 관심종목 그룹 생성에 성공하셨습니다."),
    CRYPTO_INTEREST_GROUP_UPDATE(message = "암호화폐 관심종목 그룹 수정에 성공하셨습니다."),
    CRYPTO_INTEREST_GROUP_DELETE(message = "암호화폐 관심종목 그룹 삭제에 성공하셨습니다."),
    CRYPTO_INTEREST_GROUP_REORDER(message = "암호화폐 관심종목 그룹 순서 변경에 성공하셨습니다."),
    CRYPTO_INTEREST_ITEM_LIST(message = "암호화폐 관심종목 목록 조회에 성공하셨습니다."),
    CRYPTO_INTEREST_ITEM_ADD(message = "암호화폐 관심종목 추가에 성공하셨습니다."),
    CRYPTO_INTEREST_ITEM_DELETE(message = "암호화폐 관심종목 삭제에 성공하셨습니다."),
    CRYPTO_INTEREST_ITEM_REORDER(message = "암호화폐 관심종목 순서 변경에 성공하셨습니다."),
    CRYPTO_INTEREST_ITEM_STREAM(message = "실시간 암호화폐 관심종목 조회에 성공하셨습니다."),
}