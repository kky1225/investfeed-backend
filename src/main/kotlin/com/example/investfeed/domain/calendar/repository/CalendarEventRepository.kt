package com.example.investfeed.domain.calendar.repository

import com.example.investfeed.domain.calendar.entity.CalendarEventEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CalendarEventRepository : JpaRepository<CalendarEventEntity, Long> {
    fun findByYearAndMonth(year: Int, month: Int): List<CalendarEventEntity>
    fun findByTypeAndYearBetween(type: String, startYear: Int, endYear: Int): List<CalendarEventEntity>
    fun findByYearAndSource(year: Int, source: String): List<CalendarEventEntity>

    @Query("SELECT COUNT(e) FROM CalendarEventEntity e WHERE e.year = :year AND e.month = :month AND e.source IN ('FRED', 'HOLIDAY') AND e.type IN ('INDICATOR', 'HOLIDAY')")
    fun countFrozenApiEvents(year: Int, month: Int): Long

    @Modifying
    @Query("DELETE FROM CalendarEventEntity e WHERE e.year = :year AND e.month = :month AND e.type IN ('INDICATOR', 'HOLIDAY')")
    fun deleteApiEventsByYearAndMonth(year: Int, month: Int)

    fun findByYearAndMonthAndTypeIn(year: Int, month: Int, types: Collection<String>): List<CalendarEventEntity>
}
