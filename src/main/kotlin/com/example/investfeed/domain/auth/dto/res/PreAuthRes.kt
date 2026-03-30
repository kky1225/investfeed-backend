package com.example.investfeed.domain.auth.dto.res

data class PreAuthRes(
    val totpRequired: Boolean = true,
    val totpSetupRequired: Boolean = false
)
