package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantStockAlias
import org.springframework.data.jpa.repository.JpaRepository

interface AssistantStockAliasRepository : JpaRepository<AssistantStockAlias, Long> {
    fun findByAliasIgnoreCase(alias: String): AssistantStockAlias?
    fun findAllByOrderByAliasAsc(): List<AssistantStockAlias>
}
