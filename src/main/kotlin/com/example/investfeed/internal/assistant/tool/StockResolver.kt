package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.assistant.repository.AssistantStockAliasRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.stock.repository.StockMasterRepository
import com.example.investfeed.domain.us.stock.entity.UsStockMaster
import com.example.investfeed.domain.us.stock.repository.UsStockMasterRepository
import com.example.investfeed.upbit.market.client.MarketClient
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class StockResolver(
    private val aliasRepository: AssistantStockAliasRepository,
    private val stockMasterRepository: StockMasterRepository,
    private val usStockMasterRepository: UsStockMasterRepository,
    private val memberHoldingRepository: MemberHoldingRepository,
    private val marketClient: MarketClient,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val MAX_CANDIDATES = 5
        private val KR_CODE = Regex("^\\d[0-9A-Z]{5}$")   // 신규 ETF 는 영문 섞인 코드 (0069M0) — 후보 버튼이 코드로 다시 찾는다
        private val US_TICKER = Regex("^[A-Z]{1,5}$")
        private val CRYPTO_CODE = Regex("^KRW-[A-Z0-9]{2,10}$")
        private val COIN_CACHE_TTL: Duration = Duration.ofHours(1)
        private val MASTER_CACHE_TTL: Duration = Duration.ofHours(1)
        private val SPACES = Regex("\\s+")
        const val MIN_SIMILARITY = 0.45
    }

    /** 이름 대조용 종목 (keys = 공백 없애고 소문자로 바꾼 이름들 — 미국은 한글명·영문명·티커) */
    private data class Named(val stock: ResolvedStock, val keys: List<String>)
    @Volatile private var masterCache: Pair<Instant, List<Named>>? = null

    private data class Coin(val market: String, val koreanName: String, val symbol: String)
    @Volatile private var coinCache: Pair<Instant, List<Coin>>? = null

    fun resolve(query: String, memberId: Long, marketHint: StockMarket? = null): ResolvedStock {
        val q = query.trim()
        if (q.isEmpty()) throw ToolException("종목을 입력해 주세요")

        exactByCode(q, marketHint)?.let { return it }
        exactByName(q, marketHint)?.let { return it }
        aliasRepository.findByAliasIgnoreCase(q)?.let { a ->
            val market = runCatching { StockMarket.valueOf(a.market) }.getOrNull()
            if (market != null && (marketHint == null || marketHint == market)) exactByCode(a.stkCd, market)?.let { return it }
        }
        val candidates = partial(q, marketHint).distinctBy { it.market to it.code }
        if (candidates.isEmpty()) suggest(q, marketHint)
        if (candidates.size == 1) return candidates.first()

        val heldCodes = memberHoldingRepository.findByMemberId(memberId).map { it.stkCd }.toSet()
        val held = candidates.filter { it.assetCode in heldCodes }
        if (held.size == 1) return held.first()
        throw ToolException(
            "종목 후보가 여럿입니다: $q",
            candidates.take(MAX_CANDIDATES).map { StockCandidate(it.code, it.name, it.market) },
            query = q,
        )
    }

    private fun exactByCode(q: String, hint: StockMarket?): ResolvedStock? {
        val upper = q.uppercase()
        if ((hint == null || hint == StockMarket.KR) && KR_CODE.matches(upper)) {
            stockMasterRepository.findByStkCdIn(listOf(upper)).firstOrNull()?.let { return ResolvedStock(it.stkCd, it.stkNm, StockMarket.KR) }
        }
        if ((hint == null || hint == StockMarket.CRYPTO) && CRYPTO_CODE.matches(upper)) {
            coins().firstOrNull { it.market == upper }?.let { return ResolvedStock(it.market, it.koreanName, StockMarket.CRYPTO) }
        }
        if ((hint == null || hint == StockMarket.US) && US_TICKER.matches(upper)) {
            usStockMasterRepository.findByStkCdIn(listOf(upper)).firstOrNull()?.let { return us(it) }
        }
        return null
    }

    private fun exactByName(q: String, hint: StockMarket?): ResolvedStock? {
        if (hint == null || hint == StockMarket.KR) {
            stockMasterRepository.findTop20ByStkNmContainingIgnoreCase(q).firstOrNull { it.stkNm.equals(q, ignoreCase = true) }
                ?.let { return ResolvedStock(it.stkCd, it.stkNm, StockMarket.KR) }
        }
        if (hint == null || hint == StockMarket.CRYPTO) {
            val upper = q.uppercase()
            coins().firstOrNull { it.koreanName == q || it.symbol == upper }?.let { return ResolvedStock(it.market, it.koreanName, StockMarket.CRYPTO) }
        }
        if (hint == null || hint == StockMarket.US) {
            usStockMasterRepository.search(q, PageRequest.of(0, 20)).firstOrNull { it.stkNm.equals(q, true) || it.stkEnm.equals(q, true) }
                ?.let { return us(it) }
        }
        return null
    }

    private fun partial(q: String, hint: StockMarket?): List<ResolvedStock> {
        val out = mutableListOf<ResolvedStock>()
        if (hint == null || hint == StockMarket.KR) {
            out += stockMasterRepository.findTop20ByStkNmContainingIgnoreCase(q).map { ResolvedStock(it.stkCd, it.stkNm, StockMarket.KR) }
        }
        if (hint == null || hint == StockMarket.CRYPTO) {
            out += coins().filter { it.koreanName.contains(q) }.map { ResolvedStock(it.market, it.koreanName, StockMarket.CRYPTO) }
        }
        if ((hint == null || hint == StockMarket.US) && q.length >= 2) {
            out += usStockMasterRepository.search(q, PageRequest.of(0, 20)).map { us(it) }
        }
        return out
    }

    /**
     * 이름 일부로도 못 찾았을 때: 공백으로 나눈 단어가 모두 들어 있는 종목("kodex 2차전지 레버리지" → KODEX 2차전지산업레버리지),
     * 그래도 없으면 이름이 비슷한 종목을 후보로 되묻는다. 정확히 일치하지 않으니 1개여도 바로 확정하지 않는다 (2026-10-27)
     */
    private fun suggest(q: String, hint: StockMarket?): Nothing {
        val named = named(hint)
        val tokens = q.lowercase().split(SPACES).filter { it.isNotEmpty() }
        val byTokens = named.filter { n -> n.keys.any { k -> tokens.all { k.contains(it) } } }.sortedBy { it.stock.name.length }
        val hits = byTokens.ifEmpty {
            val nq = norm(q)
            named.map { n -> n to n.keys.maxOf { similarity(nq, it) } }
                .filter { (_, score) -> score >= MIN_SIMILARITY }
                .sortedByDescending { (_, score) -> score }
                .map { (n, _) -> n }
        }
        if (hits.isEmpty()) throw ToolException("종목을 찾지 못했습니다: $q")
        throw ToolException(
            "정확히 일치하는 종목이 없습니다: $q",
            hits.map { it.stock }.distinctBy { it.market to it.code }.take(MAX_CANDIDATES).map { StockCandidate(it.code, it.name, it.market, similar = true) },
            query = q,
        )
    }

    private fun named(hint: StockMarket?): List<Named> {
        val coins = coins().map { Named(ResolvedStock(it.market, it.koreanName, StockMarket.CRYPTO), listOf(norm(it.koreanName), norm(it.symbol))) }
        return (masters() + coins).filter { hint == null || it.stock.market == hint }
    }

    /** 국내·미국 종목 마스터 전체 (국내 4천여·미국 1만 3천여 건) — 비슷한 이름 계산용, 1시간 캐시 */
    private fun masters(): List<Named> {
        val cached = masterCache
        if (cached != null && cached.first.plus(MASTER_CACHE_TTL).isAfter(Instant.now())) return cached.second
        val list = runCatching {
            stockMasterRepository.findAll().map { Named(ResolvedStock(it.stkCd, it.stkNm, StockMarket.KR), listOf(norm(it.stkNm))) } +
                usStockMasterRepository.findAll().map { m -> Named(us(m), listOfNotNull(m.stkNm, m.stkEnm, m.stkCd).map(::norm)) }
        }.onFailure { log.error(it) { "종목 마스터 조회 실패 (비슷한 종목 후보)" } }
            .getOrDefault(cached?.second ?: emptyList())
        masterCache = Instant.now() to list
        return list
    }

    fun norm(s: String) = s.replace(SPACES, "").lowercase()

    /** 글자 2개씩 묶은 조각의 겹침 비율 (Dice 계수, 0~1). 오타·일부 누락에 강하다 */
    fun similarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val x = if (a.length < 2) listOf(a) else a.windowed(2)
        val y = (if (b.length < 2) listOf(b) else b.windowed(2)).toMutableList()
        val total = x.size + y.size
        val hit = x.count { y.remove(it) }
        return 2.0 * hit / total
    }

    /** 미국 종목 표기: 목록은 ETF 만 티커, 상세형은 title (기존 웹 화면과 같은 규칙) */
    private fun us(m: UsStockMaster): ResolvedStock {
        val fullName = m.stkNm ?: m.stkEnm ?: m.stkCd
        val etf = m.isEtf == "Y"
        return ResolvedStock(m.stkCd, if (etf) m.stkCd else fullName, StockMarket.US, m.stexTp, fullName = fullName, etf = etf)
    }

    private fun coins(): List<Coin> {
        val cached = coinCache
        if (cached != null && cached.first.plus(COIN_CACHE_TTL).isAfter(Instant.now())) return cached.second
        val list = runCatching { marketClient.getKrwMarkets().map { Coin(it.market, it.korean_name, it.market.removePrefix("KRW-")) } }
            .onFailure { log.error(it) { "업비트 마켓 목록 조회 실패 (종목 해석)" } }
            .getOrDefault(cached?.second ?: emptyList())
        coinCache = Instant.now() to list
        return list
    }
}
