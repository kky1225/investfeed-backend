package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantBriefing
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface AssistantBriefingRepository : JpaRepository<AssistantBriefing, Long> {
    fun findFirstByBriefingDateAndTypeOrderByVersionDesc(briefingDate: LocalDate, type: String): AssistantBriefing?
}