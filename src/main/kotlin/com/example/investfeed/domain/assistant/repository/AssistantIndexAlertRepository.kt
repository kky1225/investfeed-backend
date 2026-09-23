package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantIndexAlert
import org.springframework.data.jpa.repository.JpaRepository

interface AssistantIndexAlertRepository : JpaRepository<AssistantIndexAlert, Long>
