package com.example.investfeed.domain.calendar.service

import com.example.investfeed.domain.calendar.dto.res.*
import com.example.investfeed.domain.calendar.entity.CalendarEventEntity
import com.example.investfeed.domain.calendar.repository.CalendarEventRepository
import com.example.investfeed.ecos.client.EcosClient
import com.example.investfeed.fred.client.FredClient
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.global.holiday.MarketHolidayRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.example.investfeed.domain.calendar.dto.req.ManualCalendarEventReq
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@Service
class EconomicCalendarService(
    private val ecosClient: EcosClient,
    private val fredClient: FredClient,
    private val marketHolidayRepository: MarketHolidayRepository,
    private val calendarEventRepository: CalendarEventRepository,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}
    private val CACHE_PREFIX = RedisKeyPrefix.ECONOMIC_CALENDAR.prefix
    private val CACHE_TTL = 24L * 60L
    private val FREEZE_GRACE_MONTHS = 1L
    private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    data class KrIndicatorDef(val tableCode: String, val itemCode: String, val name: String, val unit: String, val frequency: String, val computeYoY: Boolean = false)
    data class UsIndicatorDef(val seriesId: String, val name: String, val unit: String, val frequency: String = "M")
    data class FredReleaseSeries(val name: String, val seriesId: String, val unit: String)

    companion object {
        val KR_INDICATORS = listOf(
            KrIndicatorDef("722Y001", "0101000", "기준금리", "%", "D"),
            KrIndicatorDef("901Y009", "0", "소비자물가지수(전년동월비)", "%", "M", computeYoY = true),
            KrIndicatorDef("200Y102", "10111", "GDP 성장률", "%", "Q"),
            KrIndicatorDef("731Y003", "0000003", "원/달러 환율", "원", "D"),
        )

        val US_INDICATORS = listOf(
            UsIndicatorDef("DFEDTARU", "미국 기준금리", "%", "D"),
            UsIndicatorDef("CPIAUCSL", "소비자물가지수(전년동월비)", "%", "M"),
            UsIndicatorDef("A191RL1Q225SBEA", "GDP 성장률", "%", "Q"),
            UsIndicatorDef("UNRATE", "실업률", "%", "M"),
            UsIndicatorDef("PAYEMS", "비농업 신규고용", "천 명", "M"),
            UsIndicatorDef("ICSA", "신규 실업수당 청구건수", "건", "W"),
            UsIndicatorDef("PCEPI", "PCE 물가지수(전년동월비)", "%", "M"),
            UsIndicatorDef("T10Y2Y", "장단기 금리차", "%", "D"),
        )

        // FRED units=pc1 (전년동월비 %) 적용 대상 시리즈
        val FRED_PC1_SERIES = setOf("CPIAUCSL", "PCEPI")
        // FRED units=chg (전월 대비 증감) 적용 대상 시리즈 — level 대신 MoM 증감 표시
        val FRED_CHG_SERIES = setOf("PAYEMS")

        // FRED release_id → 캘린더에 표시할 시리즈 목록 (한 릴리즈가 여러 지표 포함 가능)
        val FRED_RELEASE_SERIES = mapOf(
            50 to listOf(
                FredReleaseSeries("비농업고용", "PAYEMS", "천 명"),
                FredReleaseSeries("실업률", "UNRATE", "%"),
            ),
            10 to listOf(FredReleaseSeries("소비자물가지수(전년동월비)", "CPIAUCSL", "%")),
            53 to listOf(FredReleaseSeries("GDP 성장률", "A191RL1Q225SBEA", "%")),
            54 to listOf(FredReleaseSeries("PCE 물가지수(전년동월비)", "PCEPI", "%")),
            180 to listOf(FredReleaseSeries("신규 실업수당", "ICSA", "건")),
        )
        // 주간 릴리즈(매주 표시)
        val FRED_WEEKLY_RELEASES = setOf(180)

        // 히스토리 차트용: 시리즈 → release_id (realtime 기반 발표일 매핑)
        val FRED_SERIES_RELEASE_ID = mapOf(
            "CPIAUCSL" to 10,
            "A191RL1Q225SBEA" to 53,
            "UNRATE" to 50,
            "PAYEMS" to 50,
            "PCEPI" to 54,
            "ICSA" to 180,
        )
    }

    // ==================================================================================
    // 백그라운드 캐시 동기화 (스케줄러에서 호출)
    // ==================================================================================

    /**
     * 현재월 이벤트 캐시가 Redis 에 이미 존재하는지 확인한다.
     * 기동 시 `@PostConstruct` warming 이 불필요한지 판단하는 용도.
     * TTL 이 길어(24시간) 정상 운영 시 항상 true. 서버 24시간 이상 down 후 재기동일 때만 false.
     */
    fun isCacheWarm(): Boolean {
        val now = YearMonth.now()
        val key = "${CACHE_PREFIX}events:${now.year}:${now.monthValue}"
        return redisTemplate.hasKey(key)
    }

    /**
     * 4개월분(현재 기준 -1 ~ +2) 이벤트 + 지표 카드를 외부 API 에서 가져와 Redis 에 저장한다.
     * 스케줄러가 30분 주기로 호출하여 Redis 를 항상 warm 상태로 유지한다.
     *
     * 범위: freeze 유예 기간(FREEZE_GRACE_MONTHS) 내 과거 + 현재 + 미래 2개월
     * 예) 4월 기준 → 3월, 4월, 5월, 6월
     */
    fun syncCurrentData() {
        val now = YearMonth.now()
        val updatedAt = LocalDateTime.now().format(DATETIME_FMT)

        // 지표 카드 갱신
        runCatching {
            val indicators = fetchIndicators()
            cacheIndicators("${CACHE_PREFIX}indicators", indicators.copy(lastUpdated = updatedAt))
        }.onFailure { log.error { "일정 동기화 - 지표 카드 갱신 실패: ${it.message}" } }

        // 이벤트 갱신 (현재 -FREEZE_GRACE_MONTHS ~ +2)
        // release_dates 는 월별 반복 조회 대신 전 구간을 릴리즈당 1회로 통합 조회해 재사용한다.
        val prefetchedReleaseDates = prefetchReleaseDates(now.minusMonths(FREEZE_GRACE_MONTHS), now.plusMonths(2))

        for (offset in -FREEZE_GRACE_MONTHS..2L) {
            val target = now.plusMonths(offset)
            runCatching {
                val events = fetchApiEvents(target.year, target.monthValue, prefetchedReleaseDates)
                val result = CalendarEventsRes(events = events.sortedBy { it.date }, lastUpdated = updatedAt)
                cacheEvents("${CACHE_PREFIX}events:${target.year}:${target.monthValue}", result)
            }.onFailure { log.error { "일정 동기화 - ${target.year}-${target.monthValue} 이벤트 갱신 실패: ${it.message}" } }
        }
    }

    /**
     * 동기화 대상 전 구간(from ~ to)의 release 발표일을 릴리즈당 1회 통합 조회한다.
     *
     * FRED /fred/release/dates 는 realtime range 를 넓혀도 결과가 월별 조회 결과의 합집합과
     * 일치하므로, 월 단위 반복 호출(릴리즈수 x 개월수)을 릴리즈수 만큼으로 줄일 수 있다.
     * 실제 사용은 fetchFredEvents 가 월 구간으로 잘라서 한다.
     *
     * 반환 map 에 없는 releaseId 는 fetchFredEvents 가 기존 월 단위 조회로 폴백하므로,
     * 통합 조회가 실패해도 결과와 실패 시 거동이 기존과 동일하게 유지된다.
     */
    private fun prefetchReleaseDates(from: YearMonth, to: YearMonth): Map<Int, List<String>> {
        val start = from.atDay(1).format(DATE_FMT)
        val end = to.atEndOfMonth().format(DATE_FMT)

        return FRED_RELEASE_SERIES.keys.mapNotNull { releaseId ->
            runCatching {
                val dates = fredClient.getReleaseDatesByReleaseId(
                    releaseId = releaseId,
                    realtimeStart = start,
                    realtimeEnd = end,
                    sortOrder = "asc",
                    includeReleaseDatesWithNoData = true,
                ).release_dates?.mapNotNull { it.date } ?: return@runCatching null
                releaseId to dates
            }.onFailure {
                log.warn { "release_dates 통합 조회 실패 - 월 단위 조회로 폴백 (releaseId=$releaseId): ${it.message}" }
            }.getOrNull()
        }.toMap()
    }

    // ==================================================================================
    // 지표 카드 (최신 값 + 전기 대비 변동)
    // ==================================================================================

    fun listIndicators(): EconomicIndicatorsRes {
        val cacheKey = "${CACHE_PREFIX}indicators"
        redisTemplate.opsForValue().get(cacheKey)?.let {
            runCatching { return objectMapper.readValue(it, EconomicIndicatorsRes::class.java) }
        }
        val updatedAt = LocalDateTime.now().format(DATETIME_FMT)
        return fetchIndicators().copy(lastUpdated = updatedAt).also { cacheIndicators(cacheKey, it) }
    }

    private fun cacheIndicators(key: String, res: EconomicIndicatorsRes) {
        runCatching {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(res), CACHE_TTL, TimeUnit.MINUTES)
        }.onFailure { log.warn { "지표 캐시 저장 실패: ${it.message}" } }
    }

    private fun fetchIndicators(): EconomicIndicatorsRes {
        val indicators = mutableListOf<EconomicIndicator>()
        indicators.addAll(fetchKrIndicators())
        indicators.addAll(fetchUsIndicators())
        return EconomicIndicatorsRes(indicators = indicators)
    }

    private fun fetchKrIndicators(): List<EconomicIndicator> {
        val today = LocalDate.now()
        return KR_INDICATORS.map { def ->
            // YoY 계산 지표는 13개월 전 지수도 필요 → 과거 범위를 2년 이상 확보
            val rangeStart = if (def.computeYoY) today.minusMonths(25) else today.minusYears(1)
            val (startDate, endDate) = ecosRange(def.frequency, rangeStart, today)
            val maxRows = if (def.frequency == "D") 300 else 100
            val rows = ecosClient.getStatistics(def.tableCode, def.frequency, startDate, endDate, def.itemCode, maxRows = maxRows)
                ?.statisticSearch?.row
                ?: throw IllegalStateException("ECOS 응답이 비어있습니다: ${def.name}")
            if (rows.isEmpty()) throw IllegalStateException("ECOS 데이터가 없습니다: ${def.name}")

            if (def.computeYoY) {
                val yoyPoints = indexToYoY(rows)
                if (yoyPoints.isEmpty()) throw IllegalStateException("YoY 계산 결과 없음: ${def.name}")
                val latest = yoyPoints.last()
                val prev = yoyPoints.getOrNull(yoyPoints.size - 2)
                val change = computeChange(latest.value, prev?.value, def.unit)
                EconomicIndicator(
                    code = def.tableCode, name = def.name, country = "KR",
                    latestValue = latest.value, latestDate = latest.date,
                    unit = def.unit, change = change,
                )
            } else {
                val latest = rows.last()
                val prev = rows.getOrNull(rows.size - 2)
                val change = computeChange(latest.DATA_VALUE, prev?.DATA_VALUE, def.unit)
                EconomicIndicator(
                    code = def.tableCode, name = def.name, country = "KR",
                    latestValue = latest.DATA_VALUE ?: "-",
                    latestDate = latest.TIME ?: "-",
                    unit = def.unit, change = change,
                )
            }
        }
    }

    private fun fetchUsIndicators(): List<EconomicIndicator> {
        val yearAgoStr = LocalDate.now().minusYears(1).format(DATE_FMT)
        return US_INDICATORS.map { def ->
            val units = when {
                def.seriesId in FRED_PC1_SERIES -> "pc1"
                def.seriesId in FRED_CHG_SERIES -> "chg"
                else -> null
            }
            val obs = fredClient.getSeriesObservations(def.seriesId, observationStart = yearAgoStr, units = units)
                ?.observations?.filter { it.value != "." }
                ?: throw IllegalStateException("FRED 응답이 비어있습니다: ${def.name}")
            if (obs.isEmpty()) throw IllegalStateException("FRED 데이터가 없습니다: ${def.name}")
            val latest = obs.last()
            val prev = obs.getOrNull(obs.size - 2)
            val latestVal = if (units == "pc1") latest.value?.toDoubleOrNull()?.let { String.format("%.1f", it) }
                            else if (units == "chg") latest.value?.toDoubleOrNull()?.toLong()?.toString()
                            else latest.value
            val prevVal = if (units == "pc1") prev?.value?.toDoubleOrNull()?.let { String.format("%.1f", it) }
                          else if (units == "chg") prev?.value?.toDoubleOrNull()?.toLong()?.toString()
                          else prev?.value
            // chg 시리즈는 latestValue 자체가 증감분 → change 필드는 불필요, 대신 previousValue로 이전값 전달
            val change = if (units == "chg") null else computeChange(latestVal, prevVal, def.unit)
            val previousValue = if (units == "chg") prevVal else null
            EconomicIndicator(
                code = def.seriesId, name = def.name, country = "US",
                latestValue = latestVal ?: "-",
                latestDate = latest.date ?: "-",
                unit = def.unit, change = change, previousValue = previousValue,
            )
        }
    }

    /** 지표 값 + 단위 포맷: 숫자는 thousand separator, 단위는 short suffix */
    private fun formatEventValue(raw: String?, unit: String): String? {
        if (raw.isNullOrBlank()) return null
        val num = raw.toDoubleOrNull() ?: return raw
        val nf = java.text.NumberFormat.getInstance(java.util.Locale.US).apply { maximumFractionDigits = 4 }
        val localized = nf.format(num)
        return when (unit) {
            "%" -> "$localized%"
            "천 명" -> "${localized}K"
            "원", "건", "" -> "$localized$unit"
            else -> "$localized $unit"
        }
    }

    private fun computeChange(latest: String?, prev: String?, unit: String): String? {
        if (latest == null || prev == null) return null
        return runCatching {
            val diff = latest.toDouble() - prev.toDouble()
            if (diff == 0.0) "-"
            else String.format("%+.2f", diff) + (if (unit == "%") "%" else "")
        }.getOrNull()
    }

    /** ECOS 월별 지수 시계열에서 전년동월비(%) 리스트 생성 — 1자리 소수 */
    private fun indexToYoY(rows: List<com.example.investfeed.ecos.dto.res.EcosStatRow>): List<IndicatorDataPoint> {
        val indexMap = rows.mapNotNull { r ->
            val t = r.TIME ?: return@mapNotNull null
            val v = r.DATA_VALUE?.toDoubleOrNull() ?: return@mapNotNull null
            t to v
        }.toMap()
        return rows.mapNotNull { r ->
            val t = r.TIME ?: return@mapNotNull null
            val cur = r.DATA_VALUE?.toDoubleOrNull() ?: return@mapNotNull null
            val prevT = shiftYyyymm(t, -12) ?: return@mapNotNull null
            val prev = indexMap[prevT] ?: return@mapNotNull null
            if (prev == 0.0) return@mapNotNull null
            val yoy = (cur / prev - 1.0) * 100.0
            IndicatorDataPoint(t, String.format("%.1f", yoy))
        }
    }

    private fun shiftYyyymm(yyyymm: String, offsetMonths: Int): String? = runCatching {
        val y = yyyymm.substring(0, 4).toInt()
        val m = yyyymm.substring(4, 6).toInt()
        val total = y * 12 + (m - 1) + offsetMonths
        "%04d%02d".format(total / 12, total % 12 + 1)
    }.getOrNull()

    private fun ecosRange(frequency: String, start: LocalDate, end: LocalDate): Pair<String, String> = when (frequency) {
        "D" -> start.format(DateTimeFormatter.ofPattern("yyyyMMdd")) to end.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        "M" -> start.format(DateTimeFormatter.ofPattern("yyyyMM")) to end.format(DateTimeFormatter.ofPattern("yyyyMM"))
        "Q" -> "${start.year}Q1" to "${end.year}Q4"
        else -> start.year.toString() to end.year.toString()
    }

    // ==================================================================================
    // 히스토리 차트 (5년치)
    // ==================================================================================

    fun getIndicatorHistory(code: String, country: String): IndicatorHistoryRes? {
        val cacheKey = "${CACHE_PREFIX}history:$country:$code"
        redisTemplate.opsForValue().get(cacheKey)?.let {
            runCatching { return objectMapper.readValue(it, IndicatorHistoryRes::class.java) }
        }
        val result = if (country == "KR") fetchKrHistory(code) else fetchUsHistory(code)
        if (result != null) {
            runCatching {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL, TimeUnit.MINUTES)
            }.onFailure { log.warn { "히스토리 캐시 저장 실패: ${it.message}" } }
        }
        return result
    }

    private fun fetchKrHistory(code: String): IndicatorHistoryRes? {
        val def = KR_INDICATORS.find { it.tableCode == code }
            ?: throw IllegalArgumentException("지원하지 않는 지표 코드입니다: $code")
        val today = LocalDate.now()
        val fiveYearsAgo = today.minusYears(5)
        // YoY 계산 지표는 5년치 + 12개월 여유 → 6년치 조회
        val rangeStart = if (def.computeYoY) today.minusYears(6) else fiveYearsAgo
        val (startDate, endDate) = ecosRange(def.frequency, rangeStart, today)
        val maxRows = if (def.frequency == "D") 2000 else 100

        val rows = ecosClient.getStatistics(def.tableCode, def.frequency, startDate, endDate, def.itemCode, maxRows = maxRows)
            ?.statisticSearch?.row
            ?: throw IllegalStateException("ECOS 히스토리 응답이 비어있습니다: ${def.name}")
        val dataPoints = if (def.computeYoY) {
            indexToYoY(rows)
        } else {
            rows.mapNotNull { row ->
                if (row.TIME != null && row.DATA_VALUE != null) IndicatorDataPoint(row.TIME, row.DATA_VALUE) else null
            }
        }

        // 한국 기준금리: 금통위 회의일 기준 샘플링 + stepAfter
        if (code == "722Y001") {
            val meetings = listRateDecisions(fiveYearsAgo.year, today.year, "RATE_DECISION")
                .map { it.format(DateTimeFormatter.ofPattern("yyyyMMdd")) }.toSet()
            if (meetings.isNotEmpty()) {
                val dataMap = dataPoints.associateBy { it.date }
                val sampled = meetings.sorted().mapNotNull { dataMap[it] }
                return IndicatorHistoryRes(code, def.name, def.unit, "stepAfter", def.frequency, sampled)
            }
        }

        return IndicatorHistoryRes(code, def.name, def.unit, "linear", def.frequency, dataPoints)
    }

    private fun fetchUsHistory(code: String): IndicatorHistoryRes? {
        val def = US_INDICATORS.find { it.seriesId == code }
            ?: throw IllegalArgumentException("지원하지 않는 지표 코드입니다: $code")

        // 미국 기준금리: DFEDTARU (일별) + FOMC 회의일 기준 샘플링
        if (code == "DFEDTARU") return fetchDfedtaruHistory(def)

        // Release_id가 매핑된 지표: FRED realtime 기반 발표일 차트
        FRED_SERIES_RELEASE_ID[code]?.let { return fetchUsHistoryByRealtime(def) }

        // 나머지 (T10Y2Y 등 직접 관측값 사용)
        val fiveYearsAgoStr = LocalDate.now().minusYears(5).format(DATE_FMT)
        val obs = fredClient.getSeriesObservations(def.seriesId, observationStart = fiveYearsAgoStr)
            ?.observations?.filter { it.value != "." }
            ?: throw IllegalStateException("FRED 히스토리 응답이 비어있습니다: ${def.name}")
        return IndicatorHistoryRes(
            code, def.name, def.unit, "linear", def.frequency,
            obs.mapNotNull { if (it.date != null && it.value != null) IndicatorDataPoint(it.date, it.value) else null }
        )
    }

    /**
     * DFEDTARU (미국 기준금리) 히스토리 - FOMC 회의일 기준 샘플링
     * FOMC 일정은 DB에 KST 기준으로 저장되어 있음 (미국 결정일 + 1일)
     * FRED DFEDTARU도 회의 다음 영업일부터 새 rate 반영 → KST 회의일 = DFEDTARU 새 rate 날짜
     */
    private fun fetchDfedtaruHistory(def: UsIndicatorDef): IndicatorHistoryRes? {
        val today = LocalDate.now()
        val fiveYearsAgo = today.minusYears(5)
        val fiveYearsAgoStr = fiveYearsAgo.format(DATE_FMT)

        val obs = fredClient.getSeriesObservations(def.seriesId, observationStart = fiveYearsAgoStr)
            ?.observations?.filter { it.value != "." }
            ?: throw IllegalStateException("FRED DFEDTARU 응답이 비어있습니다")
        val dataPoints = obs.mapNotNull { if (it.date != null && it.value != null) IndicatorDataPoint(it.date, it.value) else null }
        if (dataPoints.isEmpty()) throw IllegalStateException("FRED DFEDTARU 데이터가 없습니다")

        val meetings = listRateDecisions(fiveYearsAgo.year, today.year, "US_RATE_DECISION")
        val dataMap = dataPoints.associateBy { it.date }

        // KST 회의일 = DFEDTARU 새 rate 시작일. 주말/휴일이면 가장 가까운 이전 영업일 값 사용
        val sampled = meetings.sorted().mapNotNull { meet ->
            val point = dataMap[meet.format(DATE_FMT)] ?: (0..3).firstNotNullOfOrNull { off ->
                dataMap[meet.minusDays(off.toLong()).format(DATE_FMT)]
            }
            point?.let { IndicatorDataPoint(meet.format(DATE_FMT), it.value) }
        }

        return IndicatorHistoryRes(def.seriesId, def.name, def.unit, "stepAfter", def.frequency, sampled)
    }

    /**
     * FRED realtime 범위 조회로 모든 vintage 수신 → 실제 발표일 기준 차트 데이터 생성
     * 각 관측값의 realtime_start = FRED 수신 날짜 = 발표일
     * GDP는 advance/second/third estimate(90일 내 revision) 포함, 나머지는 최초 발표만
     */
    private fun fetchUsHistoryByRealtime(def: UsIndicatorDef): IndicatorHistoryRes? {
        // pc1/chg 시리즈는 realtime 범위 조회 불가(FRED 제약) → 6년 index 조회 후 자체 계산
        val needYoY = def.seriesId in FRED_PC1_SERIES
        val needChg = def.seriesId in FRED_CHG_SERIES
        val rangeStart = if (needYoY || needChg) LocalDate.now().minusYears(6) else LocalDate.now().minusYears(5)
        val obs = fredClient.getSeriesObservations(
            def.seriesId,
            observationStart = rangeStart.format(DATE_FMT),
            realtimeStart = rangeStart.format(DATE_FMT),
        )?.observations?.filter { it.value != "." && it.realtime_start != null && it.date != null && it.value != null }
            ?: throw IllegalStateException("FRED realtime 히스토리 응답이 비어있습니다: ${def.name}")

        val dataPoints = if (def.seriesId == "A191RL1Q225SBEA") {
            // GDP: 각 분기 최초 발표일로부터 90일 내 revision까지 포함
            obs.groupBy { it.date!! }.flatMap { (_, vintages) ->
                val sorted = vintages.sortedBy { it.realtime_start }
                val firstRelease = LocalDate.parse(sorted.first().realtime_start)
                var prevValue: String? = null
                sorted.mapNotNull { v ->
                    val days = java.time.temporal.ChronoUnit.DAYS.between(firstRelease, LocalDate.parse(v.realtime_start))
                    if (days <= 90 && v.value != prevValue) {
                        prevValue = v.value
                        IndicatorDataPoint(v.realtime_start!!, v.value!!)
                    } else null
                }
            }.sortedBy { it.date }
        } else if (needYoY) {
            // 관측월별로 vintage 정렬 → 최초(원본) + 최종(개정) 둘 다 확보
            // 최초 vintage: 발표일 + 원본 YoY (발표 당시 수치)
            // 최종 vintage: 현재 FRED 값 = 개정판 YoY (차트 메인 값)
            val grouped = obs.groupBy { it.date!! }
            val firstVintageByObs = grouped.mapValues { (_, vs) ->
                val first = vs.minBy { it.realtime_start!! }
                first.realtime_start!! to (first.value!!.toDoubleOrNull() ?: 0.0)
            }
            val latestVintageByObs = grouped.mapValues { (_, vs) ->
                vs.maxBy { it.realtime_start!! }.value!!.toDoubleOrNull() ?: 0.0
            }
            firstVintageByObs.entries.sortedBy { it.key }.mapNotNull { (obsDate, pair) ->
                val (releaseDate, originalIdx) = pair
                val prevObs = shiftObsDateMinus12(obsDate) ?: return@mapNotNull null
                val originalPrev = firstVintageByObs[prevObs]?.second ?: return@mapNotNull null
                val latestCur = latestVintageByObs[obsDate] ?: return@mapNotNull null
                val latestPrev = latestVintageByObs[prevObs] ?: return@mapNotNull null
                if (originalPrev == 0.0 || latestPrev == 0.0) return@mapNotNull null
                val originalYoy = (originalIdx / originalPrev - 1.0) * 100.0
                val latestYoy = (latestCur / latestPrev - 1.0) * 100.0
                IndicatorDataPoint(
                    date = releaseDate,
                    value = String.format("%.1f", latestYoy),
                    originalValue = String.format("%.1f", originalYoy),
                    observationDate = obsDate,
                )
            }
        } else if (needChg) {
            // 관측월별 전월 대비 증감 자체 계산. 최초 vintage(원본) + 최신 vintage(개정) 모두
            val grouped = obs.groupBy { it.date!! }
            val firstVintageByObs = grouped.mapValues { (_, vs) ->
                val first = vs.minBy { it.realtime_start!! }
                first.realtime_start!! to (first.value!!.toDoubleOrNull() ?: 0.0)
            }
            val latestVintageByObs = grouped.mapValues { (_, vs) ->
                vs.maxBy { it.realtime_start!! }.value!!.toDoubleOrNull() ?: 0.0
            }
            firstVintageByObs.entries.sortedBy { it.key }.mapNotNull { (obsDate, pair) ->
                val (releaseDate, originalCur) = pair
                val prevObs = shiftObsDateMinus1(obsDate) ?: return@mapNotNull null
                val originalPrev = firstVintageByObs[prevObs]?.second ?: return@mapNotNull null
                val latestCur = latestVintageByObs[obsDate] ?: return@mapNotNull null
                val latestPrev = latestVintageByObs[prevObs] ?: return@mapNotNull null
                val originalChg = (originalCur - originalPrev).toLong()
                val latestChg = (latestCur - latestPrev).toLong()
                IndicatorDataPoint(
                    date = releaseDate,
                    value = latestChg.toString(),
                    originalValue = originalChg.toString(),
                    observationDate = obsDate,
                )
            }
        } else {
            // 나머지: 각 관측값의 최초 발표일만 사용 (연간 개정 제외)
            val seen = mutableSetOf<String>()
            obs.sortedBy { it.realtime_start }
                .mapNotNull { v ->
                    if (v.date in seen) null
                    else { seen.add(v.date!!); IndicatorDataPoint(v.realtime_start!!, v.value!!) }
                }
        }

        // GDP는 기존 frequency=Q를 M으로 내려서 x축이 발표일로 표시되게 함
        val freq = if (def.seriesId == "A191RL1Q225SBEA") "M" else def.frequency
        return IndicatorHistoryRes(def.seriesId, def.name, def.unit, "linear", freq, dataPoints)
    }

    /** FRED 관측 date(YYYY-MM-DD) → 12개월 전 같은 월(YYYY-MM-DD) */
    private fun shiftObsDateMinus12(obsDate: String): String? = runCatching {
        val d = LocalDate.parse(obsDate)
        d.minusYears(1).format(DATE_FMT)
    }.getOrNull()

    /** FRED 관측 date(YYYY-MM-DD) → 1개월 전 같은 월(YYYY-MM-DD) */
    private fun shiftObsDateMinus1(obsDate: String): String? = runCatching {
        val d = LocalDate.parse(obsDate)
        d.minusMonths(1).format(DATE_FMT)
    }.getOrNull()

    private fun listRateDecisions(startYear: Int, endYear: Int, type: String): List<LocalDate> {
        return calendarEventRepository.findByTypeAndYearBetween(type, startYear, endYear)
            .map { it.eventDate }.distinct()
    }

    // ==================================================================================
    // 캘린더 이벤트
    // ==================================================================================

    fun listManualEvents(year: Int): List<CalendarEvent> {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        return calendarEventRepository.findByYearAndSource(year, "MANUAL")
            .map { CalendarEvent(
                id = it.id, date = it.eventDate.toString(), name = it.name,
                country = it.country, value = it.value,
                isFuture = it.eventDate.isAfter(today),
                type = it.type, source = it.source,
            ) }
            .sortedBy { it.date }
    }

    @Transactional
    fun createEvent(req: ManualCalendarEventReq): CalendarEvent {
        val date = LocalDate.parse(req.date)
        val entity = calendarEventRepository.save(
            CalendarEventEntity(
                eventDate = date,
                name = req.name,
                country = req.country,
                value = req.value,
                type = req.type,
                source = "MANUAL",
                year = date.year,
                month = date.monthValue,
            )
        )
        return entity.toCalendarEvent(LocalDate.now(ZoneId.of("Asia/Seoul")))
    }

    @Transactional
    fun updateEvent(id: Long, req: ManualCalendarEventReq): CalendarEvent {
        val entity = calendarEventRepository.findById(id)
            .orElseThrow { IllegalArgumentException("일정을 찾을 수 없습니다.") }

        if (entity.source != "MANUAL") {
            throw IllegalArgumentException("외부 API 일정은 수정할 수 없습니다.")
        }

        entity.name = req.name
        entity.country = req.country
        entity.value = req.value
        entity.type = req.type
        entity.updatedAt = LocalDateTime.now()
        calendarEventRepository.save(entity)

        return entity.toCalendarEvent(LocalDate.now(ZoneId.of("Asia/Seoul")))
    }

    @Transactional
    fun deleteEvent(id: Long) {
        val entity = calendarEventRepository.findById(id)
            .orElseThrow { IllegalArgumentException("일정을 찾을 수 없습니다.") }

        if (entity.source != "MANUAL") {
            throw IllegalArgumentException("외부 API 일정은 삭제할 수 없습니다.")
        }

        calendarEventRepository.delete(entity)
    }

    fun listEvents(year: Int, month: Int): CalendarEventsRes {
        val target = YearMonth.of(year, month)
        val threshold = YearMonth.now().minusMonths(FREEZE_GRACE_MONTHS)
        val isPast = target.isBefore(threshold)

        val cacheKey = "${CACHE_PREFIX}events:$year:$month"

        // 캐시 우선 확인
        redisTemplate.opsForValue().get(cacheKey)?.let { cached ->
            runCatching {
                val cachedRes = objectMapper.readValue(cached, CalendarEventsRes::class.java)
                return mergeManualEvents(cachedRes, year, month)
            }
        }

        // 과거 월: DB에서 조회 (없으면 API 호출 후 DB 저장)
        if (isPast) {
            return getEventsFromDbOrFreeze(year, month, cacheKey)
        }

        // 현재/미래 월: API + Redis 캐시
        return getEventsFromApi(year, month, cacheKey)
    }

    private fun getEventsFromDbOrFreeze(year: Int, month: Int, cacheKey: String): CalendarEventsRes {
        val frozenCount = calendarEventRepository.countFrozenApiEvents(year, month)

        if (frozenCount > 0) {
            // DB 확정 상태 → DB 이벤트 + MANUAL 이벤트 (enrichment 포함)
            return buildEventsFromDb(year, month, cacheKey)
        }

        // 아직 freeze 안 됨 → API 호출 후 freeze + 응답
        val apiEvents = fetchApiEvents(year, month)
        runCatching { freezeMonth(year, month, apiEvents) }
            .onFailure { log.warn { "freeze 실패 ($year-$month): ${it.message}" } }

        val updatedAt = LocalDateTime.now().format(DATETIME_FMT)
        val apiResult = CalendarEventsRes(events = apiEvents.sortedBy { it.date }, lastUpdated = updatedAt)
        cacheEvents(cacheKey, apiResult)
        return mergeManualEvents(apiResult, year, month)
    }

    private fun buildEventsFromDb(year: Int, month: Int, cacheKey: String): CalendarEventsRes {
        val dbEvents = calendarEventRepository.findByYearAndMonth(year, month)
        val today = LocalDate.now()
        val events = dbEvents.map { entity ->
            val event = entity.toCalendarEvent(today)
            if (isManualEnrichable(entity)) enrichManualEvent(event) else event
        }
        val result = CalendarEventsRes(events = events.sortedBy { it.date })
        cacheEvents(cacheKey, result)
        return result
    }

    private fun getEventsFromApi(year: Int, month: Int, cacheKey: String): CalendarEventsRes {
        val apiEvents = fetchApiEvents(year, month)
        val updatedAt = LocalDateTime.now().format(DATETIME_FMT)
        val result = CalendarEventsRes(events = apiEvents.sortedBy { it.date }, lastUpdated = updatedAt)
        cacheEvents(cacheKey, result)
        return mergeManualEvents(result, year, month)
    }

    private fun cacheEvents(key: String, res: CalendarEventsRes) {
        runCatching {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(res), CACHE_TTL, TimeUnit.MINUTES)
        }.onFailure { log.warn { "캘린더 캐시 저장 실패: ${it.message}" } }
    }

    private fun mergeManualEvents(apiResult: CalendarEventsRes, year: Int, month: Int): CalendarEventsRes {
        val manualEvents = calendarEventRepository.findByYearAndMonthAndTypeIn(year, month, setOf("RATE_DECISION", "GDP_RELEASE", "US_RATE_DECISION", "HOLIDAY"))
            .filter { it.type != "HOLIDAY" || it.source == "MANUAL" }
        if (manualEvents.isEmpty()) return apiResult

        val today = LocalDate.now()
        val enriched = manualEvents.map { entity ->
            val event = entity.toCalendarEvent(today)
            if (isManualEnrichable(entity)) enrichManualEvent(event) else event
        }
        return CalendarEventsRes(events = (apiResult.events + enriched).sortedBy { it.date }, lastUpdated = apiResult.lastUpdated)
    }

    private fun isManualEnrichable(entity: CalendarEventEntity): Boolean =
        entity.type in setOf("RATE_DECISION", "GDP_RELEASE", "US_RATE_DECISION")

    private fun fetchApiEvents(
        year: Int,
        month: Int,
        prefetchedReleaseDates: Map<Int, List<String>>? = null,
    ): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        events.addAll(fetchKrMonthlyEvents(year, month))
        events.addAll(fetchFredEvents(year, month, prefetchedReleaseDates))
        events.addAll(fetchHolidayEvents(year, month))
        return events
    }

    /** 한국 월별 지표 (CPI) — 분기/일별은 MANUAL로 별도 관리 */
    private fun fetchKrMonthlyEvents(year: Int, month: Int): List<CalendarEvent> {
        val result = mutableListOf<CalendarEvent>()
        val monthStart = LocalDate.of(year, month, 1)
        val monthEnd = monthStart.plusMonths(1).minusDays(1)

        for (def in KR_INDICATORS) {
            if (def.frequency != "M") continue
            runCatching {
                // YoY 계산 지표는 12개월 전 값도 필요 → 쿼리 범위를 1년+1개월 확장
                val rangeStart = if (def.computeYoY) monthStart.minusMonths(13) else monthStart
                val (start, end) = ecosRange(def.frequency, rangeStart, monthEnd)
                val rows = ecosClient.getStatistics(def.tableCode, def.frequency, start, end, def.itemCode)
                    ?.statisticSearch?.row ?: return@runCatching

                val targetTime = "%04d%02d".format(year, month)
                if (def.computeYoY) {
                    val yoyPoints = indexToYoY(rows).associateBy { it.date }
                    val point = yoyPoints[targetTime] ?: return@runCatching
                    val eventDate = "$year-${"%02d".format(month)}-01"
                    result.add(CalendarEvent(
                        date = eventDate, name = def.name, country = "KR",
                        value = formatEventValue(point.value, def.unit),
                        isFuture = false, type = "INDICATOR", source = "ECOS",
                    ))
                } else {
                    rows.forEach { row ->
                        if (row.TIME == null || row.DATA_VALUE == null) return@forEach
                        val eventDate = "${row.TIME.substring(0, 4)}-${row.TIME.substring(4, 6)}-01"
                        result.add(CalendarEvent(
                            date = eventDate, name = def.name, country = "KR",
                            value = formatEventValue(row.DATA_VALUE, def.unit),
                            isFuture = false, type = "INDICATOR", source = "ECOS",
                        ))
                    }
                }
            }.onFailure { log.warn { "한국 월별 지표 조회 실패 (${def.name}): ${it.message}" } }
        }
        return result
    }

    /**
     * FRED 릴리즈 이벤트:
     *  - 각 시리즈를 월 단위로 realtime range 조회 → vintage 전부 수신
     *  - 관측월별 최초 vintage(min realtime_start) 찾음
     *  - 최초 vintage가 타깃월에 있는 obs만 이벤트화 (= "이 달에 처음 발표된 관측월")
     *  - pc1/chg 값은 first vintage 지수로 자체 계산 (FRED realtime + units 조합 제약 우회)
     */
    private fun fetchFredEvents(
        year: Int,
        month: Int,
        prefetchedReleaseDates: Map<Int, List<String>>? = null,
    ): List<CalendarEvent> {
        val result = mutableListOf<CalendarEvent>()
        val today = LocalDate.now()
        val monthStart = LocalDate.of(year, month, 1)
        val monthEnd = monthStart.plusMonths(1).minusDays(1)
        // obs range: GDP revision 90일 window × 안전 여유 + 셧다운 지연 대비
        // (이 함수에선 YoY 계산을 하지 않음. FRED pc1 units 로 이미 YoY % 직접 수신)
        // 문제 생기면 24로 되돌리면 됨.
        val obsStartStr = monthStart.minusMonths(12).format(DATE_FMT)
        // realtime_end: monthEnd 가 today 이후이면 생략(FRED 기본값=CT 오늘). KST/CT 시차로 today 넘기면 0건 반환되는 이슈 회피
        val monthEndRealtimeStr: String? = if (monthEnd.isBefore(today)) monthEnd.format(DATE_FMT) else null

        val monthStartStr = monthStart.format(DATE_FMT)
        val monthEndFullStr = monthEnd.format(DATE_FMT)

        for ((releaseId, seriesList) in FRED_RELEASE_SERIES) {
            val isGdp = releaseId == 53
            val isWeekly = releaseId in FRED_WEEKLY_RELEASES

            // 1) 미래 예정일 조회 (release calendar): obs vintage 가 아직 없는 발표 예정 이벤트 생성용
            //    prefetch 된 통합 조회 결과가 있으면 해당 월 구간만 잘라 재사용하고,
            //    없으면(= 단건 호출 경로이거나 통합 조회 실패) 기존대로 월 단위로 조회한다.
            val futureReleaseDates = prefetchedReleaseDates?.get(releaseId)
                ?.filter { it in monthStartStr..monthEndFullStr }
                ?.distinct()
                ?.filter { LocalDate.parse(it).isAfter(today) }
                ?: runCatching {
                    fredClient.getReleaseDatesByReleaseId(
                        releaseId = releaseId,
                        realtimeStart = monthStartStr,
                        realtimeEnd = monthEndFullStr,
                        sortOrder = "asc",
                        includeReleaseDatesWithNoData = true,
                    )?.release_dates?.mapNotNull { it.date }?.distinct()
                        ?.filter { LocalDate.parse(it).isAfter(today) }
                        ?: emptyList()
                }.getOrElse { emptyList() }

            // 2) 과거/현재 발표 이벤트: obs vintage 기반
            for (series in seriesList) {
                runCatching {
                    val res = fredClient.getSeriesObservations(
                        series.seriesId,
                        observationStart = obsStartStr,
                        realtimeStart = obsStartStr,
                        realtimeEnd = monthEndRealtimeStr,
                    ) ?: return@runCatching
                    val obs = res.observations
                        ?.filter { it.value != "." && it.realtime_start != null && it.date != null && it.value != null }
                        ?: return@runCatching
                    if (obs.isEmpty()) return@runCatching

                    val vintagesByObs = obs.groupBy { it.date!! }

                    if (isGdp) {
                        // GDP: 각 분기마다 속보치/잠정치/확정치 모든 vintage 를 별도 이벤트로 생성.
                        // 최초 발표일로부터 90일 내 모든 vintage (값이 동일해도 발표 일정 자체는 이벤트).
                        vintagesByObs.forEach { (obsDate, vintages) ->
                            val sorted = vintages.sortedBy { it.realtime_start!! }
                            val firstReleaseDate = LocalDate.parse(sorted.first().realtime_start!!)

                            sorted.forEach { v ->
                                val rd = LocalDate.parse(v.realtime_start!!)
                                val days = ChronoUnit.DAYS.between(firstReleaseDate, rd)
                                val inTargetMonth = !rd.isBefore(monthStart) && !rd.isAfter(monthEnd)

                                if (inTargetMonth && days <= 90) {
                                    val isFuture = rd.isAfter(today)
                                    val value = if (isFuture) null else formatEventValue(v.value, series.unit)
                                    val y = obsDate.substring(0, 4)
                                    val m = obsDate.substring(5, 7).toInt()
                                    val quarter = "${y}Q${(m - 1) / 3 + 1}"

                                    result.add(CalendarEvent(
                                        date = v.realtime_start!!,
                                        name = "$quarter GDP ${resolveGdpStage(quarter, rd)}",
                                        country = "US",
                                        value = value,
                                        isFuture = isFuture,
                                        type = "INDICATOR",
                                        source = "FRED",
                                    ))
                                }
                            }
                        }
                    } else {
                        // GDP 외: 각 obs 의 최초 vintage 만 이벤트화 (기존 로직)
                        val firstVintageByObs = vintagesByObs.mapValues { (_, vs) ->
                            val min = vs.minBy { it.realtime_start!! }
                            min.realtime_start!! to min.value!!
                        }

                        val newReleases = firstVintageByObs.entries
                            .filter { (_, pair) ->
                                val rd = LocalDate.parse(pair.first)
                                !rd.isBefore(monthStart) && !rd.isAfter(monthEnd)
                            }
                            .sortedBy { it.key }

                        newReleases.forEach { (obsDate, pair) ->
                            val (releaseDateStr, firstValueStr) = pair
                            val releaseDate = LocalDate.parse(releaseDateStr)
                            val isFuture = releaseDate.isAfter(today)
                            val curVal = firstValueStr.toDoubleOrNull()

                            val rawDisplay: String? = when {
                                curVal == null -> null
                                series.seriesId in FRED_PC1_SERIES -> {
                                    val prevObs = shiftObsDateMinus12(obsDate)
                                    val prev = prevObs?.let { firstVintageByObs[it]?.second?.toDoubleOrNull() }
                                    if (prev != null && prev != 0.0) String.format("%.1f", (curVal / prev - 1.0) * 100.0) else null
                                }
                                series.seriesId in FRED_CHG_SERIES -> {
                                    val prevObs = shiftObsDateMinus1(obsDate)
                                    val prev = prevObs?.let { firstVintageByObs[it]?.second?.toDoubleOrNull() }
                                    if (prev != null) (curVal - prev).toLong().toString() else null
                                }
                                else -> firstValueStr
                            }
                            val value = if (isFuture) null else formatEventValue(rawDisplay, series.unit)

                            val displayName = when {
                                isWeekly -> "${series.name} (~${obsDate.substring(5, 7)}/${obsDate.substring(8, 10)})"
                                else -> "${obsDate.substring(0, 4)}-${obsDate.substring(5, 7)} ${series.name}"
                            }

                            result.add(CalendarEvent(
                                date = releaseDateStr,
                                name = displayName,
                                country = "US",
                                value = value,
                                isFuture = isFuture,
                                type = "INDICATOR",
                                source = "FRED",
                            ))
                        }
                    }
                }.onFailure { log.warn { "FRED ${series.seriesId} 조회 실패: ${it.message}" } }
            }

            // 3) 미래 예정일을 각 시리즈별로 이벤트 생성 (value=null)
            futureReleaseDates.forEach { rdStr ->
                seriesList.forEach { series ->
                    result.add(CalendarEvent(
                        date = rdStr,
                        name = series.name,
                        country = "US",
                        value = null,
                        isFuture = true,
                        type = "INDICATOR",
                        source = "FRED",
                    ))
                }
            }
        }
        return result
    }

    /** GDP 발표 단계 판단 (속보/잠정/확정) — 해당 분기 release_id=53 발표일 순서로 결정 */
    private fun resolveGdpStage(quarter: String, releaseDate: LocalDate): String {
        return runCatching {
            val qYear = quarter.substring(0, 4).toInt()
            val qNum = quarter.substring(5).toInt()
            // 분기 종료 후 ~6개월 내 모든 release 53 발표일 조회
            val qEnd = LocalDate.of(qYear, qNum * 3, 1).plusMonths(1).minusDays(1)
            val searchStart = qEnd.format(DATE_FMT)
            val searchEnd = releaseDate.format(DATE_FMT)

            val dates = fredClient.getReleaseDatesByReleaseId(
                releaseId = 53, realtimeStart = searchStart, realtimeEnd = searchEnd,
                sortOrder = "asc", includeReleaseDatesWithNoData = false,
            )?.release_dates?.mapNotNull { it.date }?.sorted() ?: return@runCatching "발표"

            // releaseDate 이전까지 해당 분기 데이터를 반영하는 release 개수
            var count = 0
            for (d in dates) {
                val rd = LocalDate.parse(d)
                val obsRes = fredClient.getSeriesObservations(
                    "A191RL1Q225SBEA", observationStart = qEnd.minusMonths(6).format(DATE_FMT),
                    observationEnd = d, realtimeStart = d, realtimeEnd = d,
                )
                val latestObsDate = obsRes.observations?.filter { it.value != "." }?.lastOrNull()?.date
                if (latestObsDate != null) {
                    val y = latestObsDate.substring(0, 4)
                    val m = latestObsDate.substring(5, 7).toInt()
                    val q = "${y}Q${(m - 1) / 3 + 1}"
                    if (q == quarter) count++
                }
                if (rd == releaseDate) break
            }
            when (count) { 1 -> "속보치"; 2 -> "잠정치"; else -> "확정치" }
        }.getOrElse { "발표" }
    }

    private fun fetchHolidayEvents(year: Int, month: Int): List<CalendarEvent> {
        val today = LocalDate.now()
        val monthKey = "$year${String.format("%02d", month)}"
        val holidays = marketHolidayRepository
            .findAllByMarketAndDtBetween(HolidayService.MARKET_KR, "${monthKey}01", "${monthKey}31")
        val events = holidays.map { h ->
            val dateStr = "${h.dt.substring(0, 4)}-${h.dt.substring(4, 6)}-${h.dt.substring(6, 8)}"
            CalendarEvent(
                date = dateStr, name = h.name, country = "KR", value = null,
                isFuture = LocalDate.parse(dateStr, DATE_FMT).isAfter(today),
                type = "HOLIDAY", source = "HOLIDAY",
            )
        }.toMutableList()

        if (month == 12) {
            val yyyymmdd = DateTimeFormatter.ofPattern("yyyyMMdd")
            val holidaySet = holidays.map { it.dt }.toSet()
            var closure = LocalDate.of(year, 12, 31)
            while (closure.dayOfWeek.value >= 6 || holidaySet.contains(closure.format(yyyymmdd))) {
                closure = closure.minusDays(1)
            }
            events.add(
                CalendarEvent(
                    date = closure.toString(), name = "연말 휴장일 (KRX)", country = "KR", value = null,
                    isFuture = closure.isAfter(today),
                    type = "HOLIDAY", source = "HOLIDAY",
                )
            )
        }

        return events
    }


    /**
     * MANUAL 이벤트에 API 값 채우기
     * - 미래: 그대로 반환 (발표 예정)
     * - 3개월 이내 과거: API 값만 응답에 반영 (DB 변경 X)
     * - 3개월 이전 과거: API 값으로 DB 확정 + source 전환
     */
    private fun enrichManualEvent(event: CalendarEvent): CalendarEvent {
        val eventDate = LocalDate.parse(event.date)
        val today = LocalDate.now()
        if (eventDate.isAfter(today)) return event

        // 이미 DB에 확정된 row(source가 MANUAL이 아님 + 값 존재)는 API 호출 없이 그대로 재사용
        if (event.source != "MANUAL" && !event.value.isNullOrBlank()) return event

        val eventYM = YearMonth.from(eventDate)
        val threshold = YearMonth.now().minusMonths(FREEZE_GRACE_MONTHS)
        val persistToDb = eventYM.isBefore(threshold)

        return when (event.type) {
            "RATE_DECISION" -> enrichFromEcos(event, "722Y001", "0101000", "기준금리", "%", persistToDb)
            "GDP_RELEASE" -> enrichFromEcos(event, "200Y102", "10111", null, "%", persistToDb)
            "US_RATE_DECISION" -> enrichFromFred(event, "DFEDTARU", "미국 기준금리", "%", persistToDb)
            else -> event
        }
    }

    private fun enrichFromEcos(event: CalendarEvent, tableCode: String, itemCode: String, name: String?, unit: String, persistToDb: Boolean): CalendarEvent {
        val dateStr = event.date.replace("-", "")
        // 일별 조회 시도 — daily 시계열이 아닌 지표(GDP 등)는 ECOS 가 예외를 던지므로 격리
        val dailyValue = runCatching {
            ecosClient.getStatistics(tableCode, "D", dateStr, dateStr, itemCode).statisticSearch?.row?.firstOrNull()?.DATA_VALUE
        }.getOrNull()

        // 분기 데이터 fallback (GDP 등)
        val value = dailyValue ?: runCatching {
            val eDate = LocalDate.parse(event.date)
            val quarter = "${eDate.year}Q${(eDate.monthValue - 1) / 3 + 1}"
            val prevQuarter = if (eDate.monthValue <= 3) "${eDate.year - 1}Q4"
                else "${eDate.year}Q${(eDate.monthValue - 1) / 3}"

            // 발표일 기준 더 최신 분기부터 시도 (속보치는 직전 분기 데이터)
            listOf(prevQuarter, quarter).firstNotNullOfOrNull { q ->
                runCatching {
                    ecosClient.getStatistics(tableCode, "Q", q, q, itemCode).statisticSearch?.row?.firstOrNull()?.DATA_VALUE
                }.getOrNull()
            }
        }.getOrNull() ?: return event

        val displayValue = formatEventValue(value, unit) ?: "$value$unit"
        val finalName = name ?: event.name

        if (persistToDb && event.id != null) {
            calendarEventRepository.findById(event.id).ifPresent { entity ->
                if (name != null) entity.name = name
                entity.value = displayValue
                entity.source = "ECOS"
                calendarEventRepository.save(entity)
            }
            return event.copy(name = finalName, value = displayValue, source = "ECOS")
        }
        return event.copy(name = finalName, value = displayValue)
    }

    private fun enrichFromFred(event: CalendarEvent, seriesId: String, name: String, unit: String, persistToDb: Boolean): CalendarEvent {
        val value = runCatching {
            val eventDate = LocalDate.parse(event.date)
            // 회의일 당일 또는 이전 7일 내 값 조회 (DFEDTARU는 회의 다음 영업일에 새 rate → 다양한 날짜 범위 필요)
            val obsStart = eventDate.minusDays(7).format(DATE_FMT)
            val obsEnd = eventDate.format(DATE_FMT)
            fredClient.getSeriesObservations(seriesId, observationStart = obsStart, observationEnd = obsEnd)
                .observations?.filter { it.value != "." }?.lastOrNull()?.value
        }.getOrNull() ?: return event

        val displayValue = formatEventValue(value, unit) ?: "$value$unit"

        if (persistToDb && event.id != null) {
            calendarEventRepository.findById(event.id).ifPresent { entity ->
                entity.name = name
                entity.value = displayValue
                entity.source = "FRED"
                calendarEventRepository.save(entity)
            }
            return event.copy(name = name, value = displayValue, source = "FRED")
        }
        return event.copy(name = name, value = displayValue)
    }

    // ==================================================================================
    // Freeze / Refresh (과거 월 DB 저장)
    // ==================================================================================

    @Transactional
    fun freezeMonth(year: Int, month: Int, apiEvents: List<CalendarEvent>? = null) {
        val events = apiEvents ?: fetchApiEvents(year, month)

        calendarEventRepository.deleteApiEventsByYearAndMonth(year, month)
        val entities = events.map { event ->
            CalendarEventEntity(
                eventDate = LocalDate.parse(event.date),
                name = event.name,
                country = event.country,
                value = event.value,
                type = event.type,
                source = event.source,
                year = year,
                month = month,
            )
        }
        calendarEventRepository.saveAll(entities)
        log.info { "캘린더 이벤트 freeze 완료: $year-$month (${entities.size}건)" }
    }

    @Transactional
    fun refreshEvents(year: Int, month: Int) {
        freezeMonth(year, month)
        runCatching { redisTemplate.delete("${CACHE_PREFIX}events:$year:$month") }
    }

    private fun CalendarEventEntity.toCalendarEvent(today: LocalDate): CalendarEvent = CalendarEvent(
        id = this.id,
        date = this.eventDate.format(DATE_FMT),
        name = this.name,
        country = this.country,
        value = this.value,
        isFuture = this.eventDate.isAfter(today),
        type = this.type,
        source = this.source,
    )
}
