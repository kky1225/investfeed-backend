package com.example.investfeed.domain.permission.dto.req

/**
 * 권한 카탈로그에 새 action 추가 요청.
 * 예: STOCK_RECOMMEND 에 SUBSCRIBE action 신설.
 */
data class AddPermissionActionReq(
    val action: String,
    val description: String? = null,
)
