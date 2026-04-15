package com.example.investfeed.domain.calendar.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "calendar_events")
class CalendarEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val eventDate: LocalDate,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false, length = 5)
    var country: String,

    @Column(length = 100)
    var value: String? = null,

    @Column(nullable = false, length = 20)
    var type: String, // INDICATOR, HOLIDAY, MEETING

    @Column(nullable = false, length = 20)
    var source: String, // ECOS, FRED, HOLIDAY, MANUAL

    @Column(nullable = false)
    val year: Int,

    @Column(nullable = false)
    val month: Int,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
