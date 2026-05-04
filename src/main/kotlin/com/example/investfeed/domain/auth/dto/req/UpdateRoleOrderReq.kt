package com.example.investfeed.domain.auth.dto.req

data class UpdateRoleOrderReq(
    val orders: List<RoleOrderItem>
)

data class RoleOrderItem(
    val id: Long,
    val orderIndex: Int,
)
