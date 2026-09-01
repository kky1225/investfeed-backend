package com.example.investfeed.domain.calendar.service

import com.example.investfeed.domain.calendar.entity.CalendarEventEntity
import com.example.investfeed.domain.calendar.repository.CalendarEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CalendarFreezeWriter(
    private val calendarEventRepository: CalendarEventRepository,
) {
    @Transactional
    fun replaceApiEvents(year: Int, month: Int, entities: List<CalendarEventEntity>) {
        calendarEventRepository.deleteApiEventsByYearAndMonth(year, month)
        calendarEventRepository.saveAll(entities)
    }
}
