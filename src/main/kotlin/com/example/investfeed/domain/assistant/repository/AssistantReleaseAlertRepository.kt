package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantReleaseAlert
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface AssistantReleaseAlertRepository : JpaRepository<AssistantReleaseAlert, Long> {
    fun existsByCountryAndEventDateAndEventName(country: String, eventDate: LocalDate, eventName: String): Boolean
}
