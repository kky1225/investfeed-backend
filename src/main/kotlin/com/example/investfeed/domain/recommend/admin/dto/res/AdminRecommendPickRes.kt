package com.example.investfeed.domain.recommend.admin.dto.res

import java.time.LocalDate

/**
 * 추천 시스템 모니터링 — Signal Inspector + Performance Tracking 통합 응답.
 *
 * 출처 분기:
 *  - 오늘 / date 미지정 → stock_pick (현재 상태)
 *    → pickDate/pickPrice/가격 백필 컬럼은 null
 *  - 과거 일자 → stock_pick_history
 *    → pickPrice + 가격 백필 컬럼 + 수익률 채워짐 (N영업일 경과 시)
 *
 * 22:00 RecommendScheduler 가 시스템 디폴트 (모든 옵션 ON) 가정으로 저장한 메타.
 * 사용자 응답 로직과 독립 (사용자 옵션 따라 별도 계산됨).
 */
data class AdminRecommendPickRes(
    // ─── 신호 (Signal) ───────────────────────────────────────────────────
    val stkCd: String,
    val stkNm: String,
    val marketType: String?,           // KOSPI / KOSDAQ
    val originSide: String?,           // BUY / SELL (raw classify 진영)
    val type: String,                  // raw classify 등급 (수급/백본, 모듈 보정 전)
    val effectiveType: String,         // Stage1 모듈 보정 최종 등급 (전체 모듈 ON, 매크로 제외)

    // ─── 수급 (백본 근거) — 왜 이 등급인지 ───────────────────────────────
    val backboneReason: String,        // classify 근거 한 줄 재구성
    val penfndK: Double?,              // 연기금 K (≥3.0 + B′통과 → STRONG)
    val frgnrBlocked: Boolean?,        // 외국인 반대매매 BLOCK 여부
    val frgnrOppositeK: Double?,       // 외국인 반대 K (BLOCK 강도 — 강반대 3.0↑ HOLD / 중간 1.5~3.0 방향유지)
    val frgnrMcapRatio: Double?,       // 외국인 시총비중 signed (≥0.1% STRONG / ≥0.05% 일반)
    val frgnrSameDirK: Double?,        // 외국인 동조 K (history 만)
    val priorTrendRatio: Double?,      // B′ 추세 명확성 비율 (≥0.7 STRONG 격상 게이트)
    val foreignerAligned: Boolean?,    // 옵션B: 외국인 12일 추세 동조
    val marketCap: Long?,              // 시총(억) (history 만)

    // ─── 후행 모듈 트리거 ────────────────────────────────────────────────
    // 매크로(동행지표) 는 의도적으로 제외 — 시간 lag 시 의미 변질하므로 백테스트에서 분리.
    val pvTrigger: String?,            // 'PROMOTE' / 'DEMOTE' / 'NONE'
    val maTrigger: String?,
    val vpTrigger: String?,
    val rsiTrigger: String?,
    val hl52wTrigger: String?,         // HighLow52w (누락 보강)
    val breakoutTrigger: String?,      // Breakout (신규)

    // ─── 후행 지표 raw ────────────────────────────────────────────────────
    val rsi14: Double?,
    val rsi14Breakdown70: Boolean?,
    val ma5: Double?,
    val ma20: Double?,
    val flu5Pct: Double?,              // 5거래일 (T-4영업일 대비) 등락 %
    val todayChangeRate: Double?,
    val todayVolume: Long?,
    val avg20dVolume: Long?,

    // ─── 사이징 (변동성 스케일 캡) ────────────────────────────────────────
    val realizedVol: Double?,          // 20일 실현변동성(연율, ratio) — 캡 산정 입력
    val volCapRatio: Double?,          // 적용 종목당 캡(ratio 0.05~0.10) = volCap(realizedVol)

    // ─── 52주 위치 (HighLow52wModule, Stage Analysis) ─────────────────────
    val high52w: Long?,
    val low52w: Long?,
    val distFromHigh52w: Double?,      // % (음수, 고점 대비 하락률)
    val distFromLow52w: Double?,       // % (양수, 저점 대비 상승률)
    val closeAboveMa20: Boolean?,      // 종가 > MA20 (추세 위치)

    // ─── 가격 + Performance (history 조회 시만 존재) ───────────────────────
    val pickDate: LocalDate?,          // 추천 일자 (history 만)
    val pickPrice: Long?,              // T일 종가 (history 만)
    val priceOpen1d: Long?,            // T+1일 시가 (백테스트 매수 가정가)
    val priceClose1d: Long?,           // T+1일 종가
    val priceClose5d: Long?,           // T+5일 종가
    val priceClose20d: Long?,          // T+20일 종가

    // 수익률 (priceOpen1d 대비, 서버 계산) — 매수가 = T+1 시가 가정
    val ret1d: Double?,                // (priceClose1d - priceOpen1d) / priceOpen1d × 100
    val ret5d: Double?,
    val ret20d: Double?,
)
