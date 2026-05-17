package com.example.investfeed.common.security

object Permissions {

    // ─── 관리자 ──────────────────────────────────────────────────────────────
    /** 증권사/거래소 마스터 CRUD */
    const val ADMIN_BROKER = "ADMIN_BROKER"

    /** 캘린더 데이터 관리 */
    const val ADMIN_CALENDAR = "ADMIN_CALENDAR"

    /** 회원 계정 관리 */
    const val ADMIN_MEMBER = "ADMIN_MEMBER"

    /** 메뉴 트리 + 가시성 권한 매핑 */
    const val ADMIN_MENU = "ADMIN_MENU"

    /** 스케줄러/Redis/시스템 모니터링 */
    const val ADMIN_MONITORING = "ADMIN_MONITORING"

    /** 권한 카탈로그 CRUD + 패턴/지원 action 관리 (개발자용) */
    const val ADMIN_PERMISSION_CATALOG = "ADMIN_PERMISSION_CATALOG"

    /** 역할별 권한 action 부여 매트릭스 (운영자용) */
    const val ADMIN_PERMISSION_GRANT = "ADMIN_PERMISSION_GRANT"

    /** 추천 시스템 일일 운영 모니터링 (trigger 메타, 매크로 스냅샷, 백필 진행도) */
    const val ADMIN_RECOMMEND_MONITORING = "ADMIN_RECOMMEND_MONITORING"

    /** 역할 CRUD 및 순서 */
    const val ADMIN_ROLE = "ADMIN_ROLE"

    // ─── 자산 ────────────────────────────────────────────────────────────────
    /** 주식+코인 통합 대시보드 */
    const val ASSET_DASHBOARD = "ASSET_DASHBOARD"

    /** 거래소 목록/내 거래소 */
    const val CRYPTO_BROKER = "CRYPTO_BROKER"

    /** 코인 보유/수동 보유 */
    const val CRYPTO_HOLDINGS = "CRYPTO_HOLDINGS"

    /** 증권사 목록/내 증권사 */
    const val STOCK_BROKER = "STOCK_BROKER"

    /** 주식 보유 종목/잔고/수동 보유 */
    const val STOCK_HOLDINGS = "STOCK_HOLDINGS"

    /** 여러 종목 동시 모니터링 페이지 */
    const val MULTI_VIEW = "MULTI_VIEW"

    // ─── 시장 데이터 ──────────────────────────────────────────────────────────
    /** 원자재 시세 (조회 + 실시간 스트림) */
    const val COMMODITY = "COMMODITY"

    /** 코인 시세/검색/순위 (조회 + 실시간 스트림) */
    const val CRYPTO_PUBLIC = "CRYPTO_PUBLIC"

    /** 환율/원자재 등 주요 지수 */
    const val MARKET_INDEX = "MARKET_INDEX"

    /** 주식 도메인 통합 대시보드 */
    const val STOCK_DASHBOARD = "STOCK_DASHBOARD"

    /** 종목 상세/차트/검색/스트림 */
    const val STOCK_DETAIL = "STOCK_DETAIL"

    /** 코스피/코스닥 지수 (조회 + 실시간 스트림) */
    const val STOCK_INDEX = "STOCK_INDEX"

    /** 외인/기관/개인 동향 */
    const val STOCK_INVESTOR = "STOCK_INVESTOR"

    /** 거래대금/등락률 순위 */
    const val STOCK_RANK = "STOCK_RANK"

    /** 추천 종목 */
    const val STOCK_RECOMMEND = "STOCK_RECOMMEND"

    /** 업종별 데이터 */
    const val STOCK_SECT = "STOCK_SECT"

    /** 테마별 종목 */
    const val STOCK_THEME = "STOCK_THEME"

    // ─── 유저 기능 ────────────────────────────────────────────────────────────
    /** 관심종목 그룹/항목 (코인) */
    const val CRYPTO_INTEREST = "CRYPTO_INTEREST"

    /** 관심종목 그룹/항목 (주식) */
    const val STOCK_INTEREST = "STOCK_INTEREST"

    /** 실현손익 조회/관리 (코인) */
    const val CRYPTO_REALIZED_PNL = "CRYPTO_REALIZED_PNL"

    /** 실현손익 조회/관리 (주식) */
    const val STOCK_REALIZED_PNL = "STOCK_REALIZED_PNL"

    /** 목표 설정/달성률 */
    const val GOAL = "GOAL"

    /** 리밸런싱 정책 및 알림 */
    const val REBALANCING = "REBALANCING"

    /** 알림 조회/설정 */
    const val NOTIFICATION = "NOTIFICATION"

    // ─── 공용/기타 ────────────────────────────────────────────────────────────
    /** 경제 캘린더 조회 */
    const val CALENDAR_VIEW = "CALENDAR_VIEW"

    /** 뉴스 조회 */
    const val NEWS = "NEWS"

    /** 서버 시간 조회 */
    const val TIME = "TIME"

    /** 내 사이드바 메뉴 조회 */
    const val USER_MENU = "USER_MENU"
}
