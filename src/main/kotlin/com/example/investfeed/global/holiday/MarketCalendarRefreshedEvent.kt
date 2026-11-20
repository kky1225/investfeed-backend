package com.example.investfeed.global.holiday

/**
 * 매일 휴장일 동기화(HolidayService.refreshHolidays)가 끝났음.
 * KrSessionTaskScheduler 가 받아 장 시각 변경일을 DB 에서 다시 읽고 예약을 다시 계산한다
 */
class MarketCalendarRefreshedEvent
