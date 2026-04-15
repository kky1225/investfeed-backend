package com.example.investfeed.ecos.dto.res

import com.fasterxml.jackson.annotation.JsonProperty

data class EcosStatRes(
    @JsonProperty("StatisticSearch")
    val statisticSearch: EcosStatSearch? = null
)

data class EcosStatSearch(
    val list_total_count: Int? = null,
    val row: List<EcosStatRow>? = null
)

data class EcosStatRow(
    val STAT_CODE: String? = null,
    val STAT_NAME: String? = null,
    val ITEM_CODE1: String? = null,
    val ITEM_NAME1: String? = null,
    val TIME: String? = null,
    val DATA_VALUE: String? = null,
    val UNIT_NAME: String? = null,
)
