package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantBriefingPersonal
import org.springframework.data.jpa.repository.JpaRepository

interface AssistantBriefingPersonalRepository : JpaRepository<AssistantBriefingPersonal, Long> {
    fun findByBriefingIdAndMemberId(briefingId: Long, memberId: Long): AssistantBriefingPersonal?
}
