package com.example.investfeed.domain.us.stock.service

import com.example.investfeed.domain.holding.dto.res.HoldingItem
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.toss.auth.service.TossAuthClient
import com.example.investfeed.toss.market.client.TossMarketClient
import com.example.investfeed.toss.market.dto.res.TossMarketSession
import com.example.investfeed.toss.market.dto.res.TossUsMarketCalendar
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

@Service
class UsSessionBaseService(
    private val tossAuthClient: TossAuthClient,
    private val tossMarketClient: TossMarketClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    enum class ExtSession(val label: String) { DAY("데이마켓"), PRE("프리마켓"), AFTER("애프터마켓") }

    data class SessionBase(val session: ExtSession, val baseDate: LocalDate, val closes: Map<String, Double>)

    private data class CurrentSession(val session: ExtSession, val baseDate: LocalDate, val cacheable: Boolean)

    companion object {
        private val NY: ZoneId = ZoneId.of("America/New_York")
        private val PREFIX = RedisKeyPrefix.US_REGULAR_CLOSE.prefix
        private val CALENDAR_TTL: Duration = Duration.ofMinutes(10)   // 장 운영 정보는 회원과 무관해 공유
        private val CLOSE_TTL: Duration = Duration.ofDays(3)          // 공식 종가는 바뀌지 않는다
        private val MISSING_TTL: Duration = Duration.ofHours(1)       // 토스에 없는 종목은 잠시 다시 묻지 않는다
        private val AFTER_SETTLE: Duration = Duration.ofMinutes(30)   // 장 마감 직후엔 일봉 종가 확정 전일 수 있어 저장하지 않는다
        private const val MISSING = "-"
        private const val CANDLE_COUNT = 3
    }

    @Volatile
    private var calendarCache: Pair<Instant, TossUsMarketCalendar>? = null

    /** 토스 연동 회원만. 미연동·정규장·휴장이면 null. 종목별 기준 종가는 병렬로 받는다 (처음 한 번만 토스 호출, 이후 Redis) */
    suspend fun currentBases(stkCds: Collection<String>): SessionBase? {
        val loginId = tossAuthClient.currentLoginIdOrNull() ?: return null
        if (!tossAuthClient.hasApiKey(loginId)) return null
        val tickers = stkCds.map { it.substringBefore("_").trim() }.filter { it.isNotBlank() }.distinct()
        if (tickers.isEmpty()) return null

        val current = currentSession() ?: return null
        val closes = coroutineScope {
            tickers.map { t -> async { regularClose(t, current.baseDate, current.cacheable)?.let { t to it } } }.awaitAll()
        }.filterNotNull().toMap()
        return SessionBase(current.session, current.baseDate, closes)
    }

    suspend fun withSessionBase(items: List<HoldingItem>): List<HoldingItem> {
        val base = currentBases(items.mapNotNull { it.usStkCd }) ?: return items
        return items.map { item ->
            item.usStkCd?.let { item.copy(sessionLabel = base.session.label, regularClose = base.closes[it]) } ?: item
        }
    }

    private suspend fun currentSession(now: Instant = Instant.now()): CurrentSession? {
        val cal = calendar(now) ?: return null
        val days = listOfNotNull(cal.previousBusinessDay, cal.today, cal.nextBusinessDay)
        days.forEachIndexed { i, day ->
            val date = day.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@forEachIndexed
            val prevDate = days.getOrNull(i - 1)?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            when {
                day.afterMarket.contains(now) -> {
                    val settled = day.afterMarket?.start()?.plus(AFTER_SETTLE)?.isBefore(now) == true
                    return CurrentSession(ExtSession.AFTER, date, settled)
                }
                day.dayMarket.contains(now) -> return prevDate?.let { CurrentSession(ExtSession.DAY, it, true) }
                day.preMarket.contains(now) -> return prevDate?.let { CurrentSession(ExtSession.PRE, it, true) }
            }
        }
        return null
    }

    private suspend fun calendar(now: Instant): TossUsMarketCalendar? {
        calendarCache?.let { (at, cal) -> if (at.plus(CALENDAR_TTL).isAfter(now)) return cal }
        return try {
            tossMarketClient.usMarketCalendar().result?.also { calendarCache = now to it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "토스 미국 장 운영 정보 조회 실패" }
            null
        }
    }

    private suspend fun regularClose(ticker: String, date: LocalDate, cacheable: Boolean): Double? {
        val key = "$PREFIX$ticker:$date"
        redisTemplate.opsForValue().get(key)?.let { return if (it == MISSING) null else it.toDoubleOrNull() }

        val candles = try {
            tossMarketClient.dailyCandles(ticker, CANDLE_COUNT).result?.candles.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "토스 일봉 조회 실패: $ticker" }
            return null   // 호출 실패는 저장하지 않고 다음에 다시 묻는다
        }
        val close = candles
            .firstOrNull { c -> c.timestamp?.let { ts -> runCatching { OffsetDateTime.parse(ts).atZoneSameInstant(NY).toLocalDate() }.getOrNull() } == date }
            ?.closePrice?.toDoubleOrNull()
            ?.takeIf { it > 0 }

        if (cacheable) redisTemplate.opsForValue().set(key, close?.toString() ?: MISSING, if (close == null) MISSING_TTL else CLOSE_TTL)
        return close
    }

    private fun TossMarketSession.start(): Instant? = startTime?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
    private fun TossMarketSession.end(): Instant? = endTime?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }

    private fun TossMarketSession?.contains(now: Instant): Boolean {
        val s = this?.start() ?: return false
        val e = this.end() ?: return false
        return !now.isBefore(s) && now.isBefore(e)
    }
}
