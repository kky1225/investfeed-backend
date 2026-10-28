package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.assistant.service.TemplateFormat
import com.example.investfeed.domain.cryptowatchlist.repository.CryptoWatchlistGroupRepository
import com.example.investfeed.domain.cryptowatchlist.repository.CryptoWatchlistItemRepository
import com.example.investfeed.domain.cryptowatchlist.service.CryptoWatchlistService
import com.example.investfeed.domain.watchlist.repository.WatchlistGroupRepository
import com.example.investfeed.domain.watchlist.repository.WatchlistItemRepository
import com.example.investfeed.domain.watchlist.service.WatchlistService
import org.springframework.stereotype.Service
import kotlin.math.abs

@Service
class WatchlistToolService(
    private val watchlistGroupRepository: WatchlistGroupRepository,
    private val watchlistItemRepository: WatchlistItemRepository,
    private val cryptoWatchlistGroupRepository: CryptoWatchlistGroupRepository,
    private val cryptoWatchlistItemRepository: CryptoWatchlistItemRepository,
    private val watchlistService: WatchlistService,
    private val cryptoWatchlistService: CryptoWatchlistService,
) {
    enum class WatchlistKind { STOCK, CRYPTO }

    data class GroupRow(val groupId: Long, val kind: WatchlistKind, val name: String, val count: Int)
    data class GroupListCard(val groups: List<GroupRow>, val notice: String? = null)

    data class WatchItem(
        val code: String, val name: String, val market: StockMarket, val exchange: String?, val link: String, val currency: String,
        val price: Double?, val changeRate: Double?,
        val sessionRate: Double? = null,  // 미국 데이·프리·애프터마켓 등락률 (토스 연동 회원만)
    )
    data class WatchlistCard(
        val groupId: Long, val kind: WatchlistKind, val groupName: String, val link: String,
        val sessionLabel: String?, val items: List<WatchItem>,
    )

    fun watchlist(memberId: Long, kind: WatchlistKind?, group: String?, groupId: Long?): Any {
        if (groupId != null) {
            val k = kind ?: throw ToolException("관심종목 종류를 지정해 주세요")
            val g = groups(memberId, k).find { it.groupId == groupId } ?: throw ToolException("관심종목 그룹을 찾을 수 없습니다")
            return items(memberId, g)
        }
        val groups = groups(memberId, kind)
        if (groups.isEmpty()) {
            return DataToolService.TextReply(if (kind == WatchlistKind.CRYPTO) "코인 관심종목 그룹이 없습니다." else "관심종목 그룹이 없습니다.")
        }
        if (!group.isNullOrBlank()) {
            val hits = match(groups, group)
            return when (hits.size) {
                0 -> list(groups, "'${group.trim()}' 그룹이 없습니다. 아래 그룹 중에서 골라 주세요.")
                1 -> items(memberId, hits.single())
                else -> list(hits)
            }
        }
        return if (groups.size == 1) items(memberId, groups.single()) else list(groups)
    }

    private fun groups(memberId: Long, kind: WatchlistKind?): List<GroupRow> {
        val stock = if (kind == null || kind == WatchlistKind.STOCK) {
            val gs = watchlistGroupRepository.findByMemberIdOrderByDisplayOrderAsc(memberId)
            val counts = watchlistItemRepository.findByGroupIdIn(gs.map { it.id }).groupingBy { it.groupId }.eachCount()
            gs.map { GroupRow(it.id, WatchlistKind.STOCK, it.groupNm, counts[it.id] ?: 0) }
        } else emptyList()
        val crypto = if (kind == null || kind == WatchlistKind.CRYPTO) {
            val gs = cryptoWatchlistGroupRepository.findByMemberIdOrderByDisplayOrderAsc(memberId)
            val counts = cryptoWatchlistItemRepository.findByGroupIdIn(gs.map { it.id }).groupingBy { it.groupId }.eachCount()
            gs.map { GroupRow(it.id, WatchlistKind.CRYPTO, it.groupNm, counts[it.id] ?: 0) }
        } else emptyList()
        return stock + crypto
    }

    /** 공백·대소문자 무시, "반도체 그룹"의 "그룹"도 뗀다. 정확히 같은 이름이 없으면 이름에 포함된 그룹이 하나일 때만 */
    private fun match(groups: List<GroupRow>, query: String): List<GroupRow> {
        fun norm(s: String) = s.replace(Regex("\\s+"), "").lowercase()
        val q = norm(query).removeSuffix("그룹").ifEmpty { norm(query) }
        val exact = groups.filter { norm(it.name) == q || norm(it.name) == norm(query) }
        if (exact.isNotEmpty()) return exact
        return groups.filter { norm(it.name).contains(q) }.takeIf { it.size == 1 }.orEmpty()
    }

    private fun list(groups: List<GroupRow>, notice: String? = null) = GroupListCard(groups, notice)

    private fun items(memberId: Long, g: GroupRow): WatchlistCard =
        when (g.kind) {
            WatchlistKind.STOCK -> {
                val rows = watchlistService.getItems(memberId, g.groupId)
                WatchlistCard(
                    g.groupId, g.kind, g.name, "/stock/watchlist/list/${g.groupId}",
                    sessionLabel = rows.firstNotNullOfOrNull { it.sessionLabel },
                    items = rows.map { r ->
                        // 키움 현재가는 부호가 붙어 온다 ("-70000")
                        val price = TemplateFormat.parse(r.curPrc)?.let { abs(it) }
                        if (r.stexTp != null) WatchItem(
                            r.stkCd, r.stkNm, StockMarket.US, r.stexTp, "/us-stock/detail/${r.stexTp}/${r.stkCd}", "USD",
                            price, TemplateFormat.parse(r.fluRt),
                            sessionRate = r.regularClose?.takeIf { it > 0 }?.let { close -> price?.let { (it / close - 1) * 100 } },
                        ) else WatchItem(
                            r.stkCd, r.stkNm, StockMarket.KR, r.mrktNm, "/stock/detail/${r.stkCd}", "KRW",
                            price, TemplateFormat.parse(r.fluRt),
                        )
                    },
                )
            }
            WatchlistKind.CRYPTO -> WatchlistCard(
                g.groupId, g.kind, g.name, "/crypto/watchlist/list/${g.groupId}", null,
                cryptoWatchlistService.getItems(memberId, g.groupId).map { r ->
                    WatchItem(r.market, r.koreanName, StockMarket.CRYPTO, null, "/crypto/detail/${r.market}", "KRW",
                        r.tradePrice, r.signedChangeRate?.let { it * 100 })
                },
            )
        }
}
