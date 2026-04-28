package com.example.investfeed.domain.menu.dto.req

data class UpdateMenuBrokersReq(
    val brokerIds: List<Long> = emptyList()
)
