package com.example.investfeed.domain.assistant.enum

/**
 * 비서 처리 기록(assistant_chat_log) 코드 → 관리자 화면 문구.
 *
 * 코드의 원천은 Python 비서 서비스다 (route: app/chat/context.py, 거절 사유: prompts.py RejectReason,
 * 되묻기: replies.py Missing, 차단: guard.py, 오류: interpret.py·turn.py, 도구: tools/registry.py).
 * Python 에 값이 추가되면 여기에 한 줄 추가한다. 모르는 코드는 원래 코드를 그대로 돌려준다.
 */
object ChatLogLabel {

    data class Labels(val routeLabel: String, val requestLabel: String?, val reasonLabel: String?)

    private val ROUTE = mapOf(
        "TOOL" to "정상 응답",
        "PICK" to "종목 선택",
        "REJECT" to "거절",
        "ASK_USER" to "되묻기",
        "BLOCKED" to "차단",
        "ERROR" to "오류",
    )

    private val TOOL = mapOf(
        "get_market_summary" to "국내 지수·수급",
        "get_market_investor_flow" to "시장 투자자별 매매",
        "get_stock_quote" to "종목 시세",
        "get_stock_investor_flow" to "종목 투자자별 매매",
        "get_global_indexes" to "미국 지수·금리·환율",
        "get_calendar" to "경제 일정",
        "get_recommend_list" to "추천 종목",
        "search_news" to "뉴스 검색",
        "show_my_portfolio" to "내 계좌 요약",
        "show_my_holding" to "내 보유 종목",
        "show_my_pnl" to "내 실현손익",
        "show_briefing" to "브리핑 다시 보기",
        "create_price_alert" to "목표가 알림 등록",
    )

    /** 거절: 사유 코드가 곧 "어떤 요청이었는지"를 나타내므로 요청 내용과 거절 이유로 나눠 보여준다 */
    private val REJECT = mapOf(
        "ACTION" to ("매매·판단·예측 요청" to "투자 판단·주문은 제공하지 않음"),
        "PAPER_TRADE" to ("모의투자 질문" to "모의투자는 관리자 기능이라 비서 범위 밖"),
        "GREETING" to ("인사" to "조회할 내용이 없는 인사"),
        "OUT_OF_SCOPE" to ("범위 밖 질문" to "투자 데이터와 무관한 질문"),
        "NO_TOOL" to ("해석되지 않은 질문" to "조회할 기능을 고르지 못해 범위 밖으로 처리"),
        "REFUSAL" to ("응답 거부된 질문" to "모델이 응답을 거부함"),
    )

    private val ASK = mapOf(
        "STOCK" to "종목이 빠짐",
        "PRICE" to "가격이 빠짐",
        "DIRECTION" to "방향(이상·이하)이 빠짐",
        "PERIOD" to "기간이 빠짐",
        "DATE_RANGE" to "날짜 범위가 빠짐",
    )

    private val BLOCK = mapOf(
        "RATE_MINUTE" to "분당 질문 횟수(10회) 초과",
        "COST_MONTH" to "월 비용 상한 초과",
    )

    private val ERROR = mapOf(
        "TIMEOUT" to "처리 시간 초과(20초)",
        "LLM" to "LLM 호출 실패",
    )

    fun of(route: String, reason: String?, tools: List<String>): Labels {
        val toolText = tools.map { TOOL[it] ?: it }.distinct().joinToString(", ").ifEmpty { null }
        val routeLabel = ROUTE[route] ?: route
        return when (route) {
            "TOOL", "PICK" -> Labels(routeLabel, toolText, null)
            "REJECT" -> REJECT[reason]
                ?.let { (request, why) -> Labels(routeLabel, request, why) }
                ?: Labels(routeLabel, toolText, reason)
            "ASK_USER" -> Labels(routeLabel, toolText ?: "조회 전 확인 필요", reason?.let { ASK[it] ?: it })
            "BLOCKED" -> Labels(routeLabel, "처리 전 차단", reason?.let { BLOCK[it] ?: it })
            "ERROR" -> Labels(routeLabel, toolText ?: "처리 중단", reason?.let { ERROR[it] ?: REJECT[it]?.second ?: it })
            else -> Labels(routeLabel, toolText, reason)
        }
    }
}
