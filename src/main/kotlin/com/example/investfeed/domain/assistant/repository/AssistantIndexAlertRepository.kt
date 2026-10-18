package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantIndexAlert
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface AssistantIndexAlertRepository : JpaRepository<AssistantIndexAlert, Long> {
    fun findByAlertDateAndTriggerTypeOrderByFiredAt(alertDate: LocalDate, triggerType: String): List<AssistantIndexAlert>
}
