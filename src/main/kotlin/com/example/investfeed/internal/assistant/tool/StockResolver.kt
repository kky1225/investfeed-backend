package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.assistant.repository.AssistantStockAliasRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.stock.repository.StockMasterRepository
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
        private val KR_CODE = Regex("^\\d{6}$")
        private val US_TICKER = Regex("^[A-Z]{1,5}$")
        private val CRYPTO_CODE = Regex("^KRW-[A-Z0-9]{2,10}$")
        private val COIN_CACHE_TTL: Duration = Duration.ofHours(1)
    }

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
        if (candidates.isEmpty()) throw ToolException("종목을 찾지 못했습니다: $q")
        if (candidates.size == 1) return candidates.first()

        val heldCodes = memberHoldingRepository.findByMemberId(memberId).map { it.stkCd }.toSet()
        val held = candidates.filter { it.assetCode in heldCodes }
        if (held.size == 1) return held.first()
        throw ToolException(
            "종목 후보가 여럿입니다: $q",
            candidates.take(MAX_CANDIDATES).map { StockCandidate(it.code, it.name, it.market) },
        )
    }

    private fun exactByCode(q: String, hint: StockMarket?): ResolvedStock? {
        val upper = q.uppercase()
        if ((hint == null || hint == StockMarket.KR) && KR_CODE.matches(q)) {
            stockMasterRepository.findByStkCdIn(listOf(q)).firstOrNull()?.let { return ResolvedStock(it.stkCd, it.stkNm, StockMarket.KR) }
        }
        if ((hint == null || hint == StockMarket.CRYPTO) && CRYPTO_CODE.matches(upper)) {
            coins().firstOrNull { it.market == upper }?.let { return ResolvedStock(it.market, it.koreanName, StockMarket.CRYPTO) }
        }
        if ((hint == null || hint == StockMarket.US) && US_TICKER.matches(upper)) {
            usStockMasterRepository.findByStkCdIn(listOf(upper)).firstOrNull()?.let { return ResolvedStock(it.stkCd, it.stkNm ?: it.stkCd, StockMarket.US, it.stexTp) }
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
                ?.let { return ResolvedStock(it.stkCd, it.stkNm ?: it.stkCd, StockMarket.US, it.stexTp) }
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
            out += usStockMasterRepository.search(q, PageRequest.of(0, 20)).map { ResolvedStock(it.stkCd, it.stkNm ?: it.stkCd, StockMarket.US, it.stexTp) }
        }
        return out
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
