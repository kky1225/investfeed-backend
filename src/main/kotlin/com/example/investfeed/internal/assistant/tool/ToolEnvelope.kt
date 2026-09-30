package com.example.investfeed.internal.assistant.tool

import java.time.LocalDateTime

data class ToolResponse<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: String? = null,
    val candidates: List<StockCandidate>? = null,   // 종목 후보 복수
    val asOf: LocalDateTime? = null,
    val source: String? = null,                     // 데이터 출처
) {
    companion object {
        fun <T> ok(data: T, source: String? = null, asOf: LocalDateTime = LocalDateTime.now()) = ToolResponse(true, data, null, null, asOf, source)
        fun <T> fail(error: String, candidates: List<StockCandidate>? = null) = ToolResponse<T>(false, null, error, candidates)
    }
}

enum class StockMarket { KR, US, CRYPTO }

data class StockCandidate(val code: String, val name: String, val market: StockMarket)

/** 종목명 해석 결과 */
data class ResolvedStock(
    val code: String,          // 005930 / TLT / KRW-BTC
    val name: String,
    val market: StockMarket,
    val stexTp: String? = null,   // 미국만 (ND/NY/NA)
) {
    val link: String get() = when (market) {
        StockMarket.KR -> "/stock/detail/$code"
        StockMarket.US -> "/us-stock/detail/$stexTp/$code"
        StockMarket.CRYPTO -> "/crypto/detail/$code"
    }
    /** 알림·보유 테이블의 자산 코드 (국내 _AL, 미국 _US, 코인 그대로) */
    val assetCode: String get() = when (market) {
        StockMarket.KR -> "${code}_AL"
        StockMarket.US -> "${code}_US"
        StockMarket.CRYPTO -> code
    }
}

/** 표시 도구 결과 — 카드 참조만 */
data class CardRef(val cardRef: String, val kind: String)

/** 도구 인자 오류·종목 미확정. 컨트롤러가 fail 봉투로 바꾼다 */
class ToolException(message: String, val candidates: List<StockCandidate>? = null) : RuntimeException(message)
