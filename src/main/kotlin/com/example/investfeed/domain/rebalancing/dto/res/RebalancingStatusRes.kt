package com.example.investfeed.domain.rebalancing.dto.res

data class RebalancingStatusRes(
    val setting: RebalancingSettingRes,
    val currentRatios: AssetRatioStatus,
    val overweightAssets: List<OverweightAssetItem>,
    val overweightStocks: List<OverweightStockItem>
)