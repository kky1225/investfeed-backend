package com.example.investfeed.domain.index.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 벤치마크용 지수 일별 종가 (키움 업종차트 수집).
 * 모의매매 성공 판정 시 lot별 시장지수(코스피 종목→001, 코스닥 종목→101) 동일 보유구간 비교에 사용.
 */
@Entity
@Table(
    name = "index_daily_close",
    uniqueConstraints = [UniqueConstraint(columnNames = ["inds_cd", "dt"])]
)
class IndexDailyClose(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "inds_cd", nullable = false)
    val indsCd: String, // 001=KOSPI / 101=KOSDAQ

    @Column(name = "dt", nullable = false)
    val dt: String, // YYYYMMDD

    @Column(name = "close_price", nullable = false)
    val closePrice: BigDecimal,

    @Column(name = "open_price")
    val openPrice: BigDecimal? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
