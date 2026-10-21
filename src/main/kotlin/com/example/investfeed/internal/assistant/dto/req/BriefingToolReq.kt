package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.domain.assistant.service.BriefingType
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class BriefingToolReq(
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val date: LocalDate? = null,   // 미지정 = 오늘
    val type: BriefingType = BriefingType.KR_CLOSE,
)
