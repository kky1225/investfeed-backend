package com.example.investfeed.domain.dividend.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "dividend",
    uniqueConstraints = [UniqueConstraint(columnNames = ["stkCd", "dvdnBasDt", "scrsItmsKcd"])],
    indexes = [Index(name = "idx_dividend_stk_cd", columnList = "stkCd")]
)
class StockDividend(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 10)
    val stkCd: String,

    @Column(nullable = false, length = 10)
    val type: String = "STOCK",

    @Column(nullable = false, length = 8)
    val basDt: String,

    @Column(length = 8)
    val createdDt: String? = null,

    @Column(length = 20)
    val crno: String? = null,

    @Column(length = 20)
    val isinCd: String? = null,

    @Column(length = 100)
    val stckIssuCmpyNm: String? = null,

    @Column(length = 100)
    val isinCdNm: String? = null,

    @Column(length = 10)
    val scrsItmsKcd: String? = null,

    @Column(length = 20)
    val scrsItmsKcdNm: String? = null,

    @Column(length = 20)
    val stckParPrc: String? = null,

    @Column(length = 10)
    val stckStacMd: String? = null,

    @Column(length = 8)
    val dvdnBasDt: String? = null,

    @Column(length = 8)
    val cashDvdnPayDt: String? = null,

    @Column(length = 8)
    val stckHndvDt: String? = null,

    @Column(length = 10)
    val stckDvdnRcd: String? = null,

    @Column(length = 50)
    val stckDvdnRcdNm: String? = null,

    @Column(length = 20)
    val stckGenrDvdnAmt: String? = null,

    @Column(length = 20)
    val stckGrdnDvdnAmt: String? = null,

    @Column(length = 20)
    val stckGenrCashDvdnRt: String? = null,

    @Column(length = 20)
    val stckGenrDvdnRt: String? = null,

    @Column(length = 20)
    val cashGrdnDvdnRt: String? = null,

    @Column(length = 20)
    val stckGrdnDvdnRt: String? = null,
)
