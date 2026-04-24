package com.example.investfeed.global.holiday.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * 공공데이터 포털 `SpcdeInfoService/getRestDeInfo` JSON 응답 DTO.
 *
 * 주의:
 * - 결과 0건 시 items 가 빈 문자열("") 로 올 수 있어 DTO 역직렬화 실패 가능 → ObjectMapper 에서
 *   `FAIL_ON_UNKNOWN_PROPERTIES = false` 로 두고, items 가 객체가 아닐 때는 null 로 파싱되도록
 *   별도 처리 필요 (HolidayClient 에서 items 매핑 시 try-catch 또는 JsonNode 사전 검증).
 * - item 이 1건일 때 객체 단일로 올 수 있어 ObjectMapper 에 `ACCEPT_SINGLE_VALUE_AS_ARRAY` 활성화 권장.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class HolidayApiResponse(
    val response: HolidayApiResponseBody? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HolidayApiResponseBody(
    val header: HolidayApiHeader? = null,
    val body: HolidayApiBody? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HolidayApiHeader(
    val resultCode: String? = null,
    val resultMsg: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HolidayApiBody(
    val numOfRows: Int? = null,
    val pageNo: Int? = null,
    val totalCount: Int? = null,
    val items: HolidayApiItems? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HolidayApiItems(
    val item: List<HolidayApiItem>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HolidayApiItem(
    val dateKind: String? = null,
    val dateName: String? = null,
    val isHoliday: String? = null,
    // 공공데이터 포털 JSON 응답에서 locdate 는 숫자(20260101) 로 옴 → Long 으로 받고 사용부에서 toString()
    val locdate: Long? = null,
    val seq: Int? = null,
)
