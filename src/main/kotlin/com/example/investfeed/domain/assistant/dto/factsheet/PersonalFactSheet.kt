package com.example.investfeed.domain.assistant.dto.factsheet

data class HoldingFact(
    val code: String,
    val name: String,
    val link: String? = null,
    val dayRate: Double? = null,
    val dayChange: Double? = null,
    val totalRate: Double? = null,
    val eval: Double? = null,
    val curPrc: Double? = null,
    val evalProfit: Double? = null,
)

data class BrokerHoldingsFact(
    val brokerName: String,
    val currency: String,
    val eval: Double,
    val dayChange: Double?,
    val dayRate: Double?,
    val totalRate: Double?,
    val items: List<HoldingFact>,
    val failed: Boolean = false,
)

data class RealizedFact(val monthTotalWon: Long, val byBroker: List<Pair<String, Long>>)

data class PersonalFactSheet(
    val memberId: Long,
    val krBrokers: List<BrokerHoldingsFact>? = null,
    val realized: RealizedFact? = null,
    val usBrokers: List<BrokerHoldingsFact>? = null,
    val usdKrw: Double? = null,
    val coinExchanges: List<BrokerHoldingsFact>? = null,
) {
    fun krAssetDayRate(): Double? = assetDayRate(krBrokers)
    fun usAssetDayRate(): Double? = assetDayRate(usBrokers)
    fun krDayChangeWon(): Long? = krBrokers?.filter { !it.failed }?.mapNotNull { it.dayChange }?.takeIf { it.isNotEmpty() }?.sum()?.let { Math.round(it) }
    fun usDayChangeUsd(): Double? = usBrokers?.filter { !it.failed }?.mapNotNull { it.dayChange }?.takeIf { it.isNotEmpty() }?.sum()

    private fun assetDayRate(brokers: List<BrokerHoldingsFact>?): Double? {
        val ok = brokers?.filter { !it.failed } ?: return null
        if (ok.isEmpty()) return null
        val eval = ok.sumOf { it.eval }
        val change = ok.mapNotNull { it.dayChange }.sum()
        val base = eval - change
        return if (base > 0) change / base * 100 else null
    }

    fun allItems(brokers: List<BrokerHoldingsFact>?): List<HoldingFact> = brokers.orEmpty().filter { !it.failed }.flatMap { it.items }
}
