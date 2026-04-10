package com.example.investfeed.domain.rebalancing.dto.res

data class RebalancingSettingRes(
    val stockRatio: Int,
    val stockDirection: String,
    val cryptoRatio: Int,
    val cryptoDirection: String,
    val cashRatio: Int,
    val cashDirection: String,
    val maxStockRatio: Int
)

data class RebalancingStatusRes(
    val setting: RebalancingSettingRes,
    val currentRatios: AssetRatioStatus,
    val overweightAssets: List<OverweightAssetItem>,
    val overweightStocks: List<OverweightStockItem>
)

data class AssetRatioStatus(
    val stockRatio: Double,
    val cryptoRatio: Double,
    val cashRatio: Double,
    val stockAmount: Long,
    val cryptoAmount: Long,
    val cashAmount: Long,
    val totalAsset: Long
)

data class OverweightAssetItem(
    val assetType: String,
    val currentRatio: Double,
    val targetRatio: Int,
    val direction: String,
    val excessAmount: Long
)

data class OverweightStockItem(
    val stkCd: String,
    val stkNm: String,
    val brokerName: String,
    val currentRatio: Double,
    val maxRatio: Int,
    val curPrc: Long,
    val evltAmt: Long,
    val sellQuantity: Long,
    val sellAmount: Long
)
