package com.example.investfeed.domain.index.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "index_investor_daily",
    uniqueConstraints = [UniqueConstraint(columnNames = ["indsCd", "dt"])]
)
class IndexInvestorDaily(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val indsCd: String,

    @Column(nullable = false)
    val dt: String, // YYYYMMDD

    @Column(nullable = false)
    val indNetprps: String, // 개인순매수

    @Column(nullable = false)
    val frgnrNetprps: String, // 외국인순매수

    @Column(nullable = false)
    val orgnNetprps: String, // 기관계순매수

    @Column(nullable = false)
    val scNetprps: String = "0", // 증권순매수

    @Column(nullable = false)
    val insrncNetprps: String = "0", // 보험순매수

    @Column(nullable = false)
    val invtrtNetprps: String = "0", // 투신순매수

    @Column(nullable = false)
    val bankNetprps: String = "0", // 은행순매수

    @Column(nullable = false)
    val endwNetprps: String = "0", // 기금순매수

    @Column(nullable = false)
    val etcCorpNetprps: String = "0", // 기타법인순매수

    @Column(nullable = false)
    val samoFundNetprps: String = "0", // 사모펀드순매수

    @Column(nullable = false)
    val natnNetprps: String = "0", // 국가순매수

    @Column(nullable = false)
    val jnsinkmNetprps: String = "0", // 종신금순매수

    @Column(nullable = false)
    val nativeTrmtFrgnrNetprps: String = "0", // 내국인대우외국인순매수
)
