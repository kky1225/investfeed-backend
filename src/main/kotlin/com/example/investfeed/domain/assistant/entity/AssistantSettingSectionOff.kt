package com.example.investfeed.domain.assistant.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "assistant_setting_section_off")
@IdClass(AssistantSettingSectionOffId::class)
class AssistantSettingSectionOff(
    @Id
    @Column(name = "member_id", nullable = false)
    val memberId: Long = 0,

    @Id
    @Column(name = "section_id", nullable = false, length = 20)
    val sectionId: String = "",
)

data class AssistantSettingSectionOffId(
    val memberId: Long = 0,
    val sectionId: String = "",
) : Serializable
