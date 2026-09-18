package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantSetting
import org.springframework.data.jpa.repository.JpaRepository

interface AssistantSettingRepository : JpaRepository<AssistantSetting, Long>
