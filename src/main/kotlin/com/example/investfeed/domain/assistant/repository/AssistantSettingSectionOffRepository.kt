package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantSettingSectionOff
import com.example.investfeed.domain.assistant.entity.AssistantSettingSectionOffId
import org.springframework.data.jpa.repository.JpaRepository

interface AssistantSettingSectionOffRepository : JpaRepository<AssistantSettingSectionOff, AssistantSettingSectionOffId> {
    fun findByMemberId(memberId: Long): List<AssistantSettingSectionOff>
    fun deleteByMemberId(memberId: Long)
}