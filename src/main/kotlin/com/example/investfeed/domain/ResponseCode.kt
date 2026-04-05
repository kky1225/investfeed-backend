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

    RANK_LIST(message = "주식 순위 조회에 성공하셨습니다."),
    STOCK_DETAIL(message = "주식 상세 조회에 성공하셨습니다."),
    STOCK_DETAIL_WS(message = "실시간 주식 상세 조회에 성공하셨습니다."),
    STOCK_SEARCH(message = "종목 검색에 성공하셨습니다."),
    STOCK_PROGRAM_CHART(message = "종목 시간별 프로그램 매매 조회에 성공하셨습니다."),

    INVESTOR_LIST(message = "투자자별 목록 조회에 성공하셨습니다."),

    INVESTOR_TRADE_DAILY(message = "투자자별 일별 매매 조회에 성공하셨습니다."),
    INVESTOR_TRADE_ORGANIZE(message = "종목별 투자자 기관별 합계 요청 조회에 성공하셨습니다."),
    INVESTOR_TRADE_RANK_LIST(message = "기관/외국인 매수 상위 순위 조회에 성공하셨습니다."),

    SECT_CHART_MINUTE_LIST(message = "업종 분봉 차트 조회에 성공하셨습니다."),

    ETF_PRICE_LIST(message = "ETF 전체 시세 조회에 성공하셨습니다."),
    ETF_INFO(message = "ETF 정목 정보 조회에 성공하셨습니다."),
    ETF_TRADE_DAILY_LIST(message = "ETF 일자별 체결 조회에 성공하셨습니다."),

    HOLDING_LIST(message = "보유 주식 조회에 성공하셨습니다."),
    HOLDING_STREAM(message = "실시간 보유 주식 조회에 성공하셨습니다."),

    BROKER_LIST(message = "증권사 목록 조회에 성공하셨습니다."),
    BROKER_CREATE(message = "증권사 등록에 성공하셨습니다."),
    BROKER_UPDATE(message = "증권사 수정에 성공하셨습니다."),
    BROKER_DELETE(message = "증권사 삭제에 성공하셨습니다."),

    MY_BROKER_LIST(message = "내 증권사 목록 조회에 성공하셨습니다."),
    MY_BROKER_ADD(message = "내 증권사 추가에 성공하셨습니다."),
    MY_BROKER_REMOVE(message = "내 증권사 삭제에 성공하셨습니다."),

    MANUAL_HOLDING_LIST(message = "수동 보유주식 조회에 성공하셨습니다."),
    MANUAL_HOLDING_CREATE(message = "수동 보유주식 등록에 성공하셨습니다."),
    MANUAL_HOLDING_UPDATE(message = "수동 보유주식 수정에 성공하셨습니다."),
    MANUAL_HOLDING_DELETE(message = "수동 보유주식 삭제에 성공하셨습니다."),
    HOLDING_REORDER(message = "보유주식 순서 변경에 성공하셨습니다."),

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

    AUTH_UNAUTHORIZED(code = "AUTH_4001", message = "인증이 필요합니다."),
    AUTH_FORBIDDEN(code = "AUTH_4003", message = "접근 권한이 없습니다."),

    AUTH_REISSUE(message = "토큰 재발급에 성공하셨습니다."),
    AUTH_LOGOUT(message = "로그아웃에 성공하셨습니다."),
    AUTH_CHANGE_PASSWORD(message = "비밀번호 변경에 성공하셨습니다."),
    AUTH_LOCK(message = "계정 잠금에 성공하셨습니다."),
    AUTH_UNLOCK(message = "계정 잠금 해제에 성공하셨습니다."),
    AUTH_CHANGE_ROLE(message = "계정 권한 변경에 성공하셨습니다."),
    AUTH_CREATE_MEMBER(message = "회원 생성에 성공하셨습니다."),
    AUTH_MEMBER_LIST(message = "회원 목록 조회에 성공하셨습니다."),
    AUTH_PROFILE(message = "프로필 조회에 성공하셨습니다."),
    AUTH_PROFILE_UPDATE(message = "프로필 수정에 성공하셨습니다."),

    AUTH_TOTP_REQUIRED(message = "TOTP 인증이 필요합니다."),
    AUTH_TOTP_SETUP(message = "TOTP 설정 QR코드가 생성되었습니다."),
    AUTH_TOTP_VERIFY(message = "TOTP 인증에 성공하셨습니다."),
    AUTH_TOTP_RESET(message = "TOTP 초기화에 성공하셨습니다."),

    AUTH_SECONDARY_REQUIRED(code = "AUTH_4040", message = "2차 비밀번호 인증이 필요합니다."),
    AUTH_SECONDARY_NOT_SET(code = "AUTH_4041", message = "2차 비밀번호가 설정되지 않았습니다."),
    AUTH_SECONDARY_SETUP(message = "2차 비밀번호가 설정되었습니다."),
    AUTH_SECONDARY_CHANGE(message = "2차 비밀번호가 변경되었습니다."),
    AUTH_SECONDARY_LOCK_STATUS(message = "2차 비밀번호 잠금 상태를 조회하였습니다."),
    AUTH_SECONDARY_VERIFY(message = "2차 비밀번호 인증에 성공하셨습니다."),

    AUTH_API_KEY_LIST(message = "API Key 목록 조회에 성공하셨습니다."),
    AUTH_API_KEY_CREATE(message = "API Key 등록에 성공하셨습니다."),
    AUTH_API_KEY_DELETE(message = "API Key 삭제에 성공하셨습니다."),

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
    CRYPTO_SEARCH(message = "암호화폐 검색에 성공하셨습니다."),
    CRYPTO_RANK_LIST(message = "암호화폐 거래대금 순위 조회에 성공하셨습니다."),
    CRYPTO_RANK_STREAM(message = "실시간 암호화폐 거래대금 순위 조회에 성공하셨습니다."),

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

    NOTIFICATION_LIST(message = "알림 목록 조회에 성공하셨습니다."),
    NOTIFICATION_UNREAD_COUNT(message = "안읽은 알림 수 조회에 성공하셨습니다."),
    NOTIFICATION_READ(message = "알림 읽음 처리에 성공하셨습니다."),
    NOTIFICATION_READ_ALL(message = "전체 알림 읽음 처리에 성공하셨습니다."),

    MENU_LIST(message = "메뉴 목록 조회에 성공하셨습니다."),
    MENU_MY_LIST(message = "사용자 메뉴 목록 조회에 성공하셨습니다."),
    MENU_CREATE(message = "메뉴 생성에 성공하셨습니다."),
    MENU_UPDATE(message = "메뉴 수정에 성공하셨습니다."),
    MENU_DELETE(message = "메뉴 삭제에 성공하셨습니다."),
    MENU_STRUCTURE_UPDATE(message = "메뉴 구조 변경에 성공하셨습니다."),
    MENU_PERMISSION_UPDATE(message = "메뉴 권한 변경에 성공하셨습니다."),

}