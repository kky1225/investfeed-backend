package com.example.investfeed.kiwoom.time.service

import com.example.investfeed.kiwoom.time.dto.TimeNowRes
import org.springframework.stereotype.Service

@Service
class TimeService() {
    fun timeNow(): TimeNowRes {
        return TimeNowRes(
            time = System.currentTimeMillis(),
        )
    }
}