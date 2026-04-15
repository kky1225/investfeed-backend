package com.example.investfeed.domain.rebalancing.dto.req

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class RebalancingSettingReq(
    @field:Min(value = 0, message = "주식 비중은 0~100% 범위로 입력해주세요.")
    @field:Max(value = 100, message = "주식 비중은 0~100% 범위로 입력해주세요.")
    val stockRatio: Int,
    @field:NotBlank(message = "주식 방향을 선택해주세요.")
    val stockDirection: String, // MIN 또는 MAX
    @field:Min(value = 0, message = "코인 비중은 0~100% 범위로 입력해주세요.")
    @field:Max(value = 100, message = "코인 비중은 0~100% 범위로 입력해주세요.")
    val cryptoRatio: Int,
    @field:NotBlank(message = "코인 방향을 선택해주세요.")
    val cryptoDirection: String,
    @field:Min(value = 0, message = "현금 비중은 0~100% 범위로 입력해주세요.")
    @field:Max(value = 100, message = "현금 비중은 0~100% 범위로 입력해주세요.")
    val cashRatio: Int,
    @field:NotBlank(message = "현금 방향을 선택해주세요.")
    val cashDirection: String,
    @field:Min(value = 1, message = "종목 최대 비중은 1~100% 범위로 입력해주세요.")
    @field:Max(value = 100, message = "종목 최대 비중은 1~100% 범위로 입력해주세요.")
    val maxStockRatio: Int
)
