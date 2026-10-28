package com.example.investfeed.domain.watchlist.service

import com.example.investfeed.domain.watchlist.dto.req.AddItemReq
import com.example.investfeed.domain.us.stock.service.UsSessionBaseService
import com.example.investfeed.domain.watchlist.dto.req.CreateGroupReq
import com.example.investfeed.domain.watchlist.dto.req.ReorderGroupsReq
import com.example.investfeed.domain.watchlist.dto.req.ReorderItemsReq
import com.example.investfeed.domain.watchlist.dto.req.UpdateGroupReq
import com.example.investfeed.domain.watchlist.dto.res.WatchlistGroupRes
import com.example.investfeed.domain.watchlist.dto.res.WatchlistItemRes
import com.example.investfeed.domain.watchlist.entity.WatchlistGroup
import com.example.investfeed.domain.watchlist.entity.WatchlistItem
import com.example.investfeed.domain.watchlist.repository.WatchlistGroupRepository
import com.example.investfeed.domain.watchlist.repository.WatchlistItemRepository
import com.example.investfeed.domain.stock.repository.StockMasterRepository
import com.example.investfeed.domain.us.stock.service.UsEtfService
import com.example.investfeed.kiwoom.socket.KiwoomStreamClient
import com.example.investfeed.kiwoom.socket.dto.KiwoomUsStreamItem
import com.example.investfeed.kiwoom.socket.dto.StreamEntry
import com.example.investfeed.kiwoom.socket.dto.StreamMarket
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import com.example.investfeed.kiwoom.us.stock.client.UsStockClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoReq
import com.example.investfeed.kiwoom.us.stock.dto.res.KiwoomUsStockInfoRes
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlinx.coroutines.runBlocking

@Service
@Transactional
class WatchlistService(
    private val kiwoomStreamClient: KiwoomStreamClient,
    private val groupRepository: WatchlistGroupRepository,
    private val itemRepository: WatchlistItemRepository,
    private val stockClient: StockClient,
    private val usStockClient: UsStockClient,
    private val stockMasterRepository: StockMasterRepository,
    private val usEtfService: UsEtfService,
    private val usSessionBaseService: UsSessionBaseService,
) {
    private val log = KotlinLogging.logger {}


    @Transactional(readOnly = true)
    fun getGroups(memberId: Long): List<WatchlistGroupRes> {
        return groupRepository.findByMemberIdOrderByDisplayOrderAsc(memberId)
            .map { WatchlistGroupRes(it.id, it.groupNm, it.displayOrder) }
    }

    fun createGroup(memberId: Long, req: CreateGroupReq): WatchlistGroupRes {
        val nextOrder = groupRepository.countByMemberId(memberId)
        val group = groupRepository.save(
            WatchlistGroup(
                memberId = memberId,
                groupNm = req.groupNm,
                displayOrder = nextOrder
            )
        )
        return WatchlistGroupRes(group.id, group.groupNm, group.displayOrder)
    }

    fun updateGroup(memberId: Long, groupId: Long, req: UpdateGroupReq) {
        val group = groupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }
        group.groupNm = req.groupNm
    }

    fun deleteGroup(memberId: Long, groupId: Long) {
        val group = groupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }
        itemRepository.deleteAllByGroupId(groupId)
        groupRepository.delete(group)
    }

    fun reorderGroups(memberId: Long, req: ReorderGroupsReq) {
        req.orderedIds.forEachIndexed { index, groupId ->
            val group = groupRepository.findById(groupId).orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
            require(group.memberId == memberId) { "접근 권한이 없습니다." }

            group.displayOrder = index
        }
    }

    @Transactional(readOnly = true)
    fun getItems(memberId: Long, groupId: Long): List<WatchlistItemRes> {
        val group = groupRepository.findById(groupId).orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }

        val items = itemRepository.findByGroupIdOrderByDisplayOrderAsc(groupId)
        val etfTickers = usEtfService.etfTickers(items.filter { it.stexTp != null }.map { it.stkCd })
        val watchlistItemRes = items.map {
            WatchlistItemRes(it.id, it.stkCd, usEtfService.displayName(it.stkCd, it.stkNm, etfTickers) ?: it.stkNm, it.stexTp)
        }

        if (watchlistItemRes.isEmpty()) {
            return emptyList()
        }

        val (usItems, krItems) = watchlistItemRes.partition { it.stexTp != null }

        if (krItems.isNotEmpty()) {
            val marketMap = stockMasterRepository.findByStkCdIn(krItems.map { it.stkCd.substringBefore("_") }.distinct())
                .associate { it.stkCd to it.mrktNm }
            krItems.forEach { it.mrktNm = marketMap[it.stkCd.substringBefore("_")] }
        }

        if (krItems.isNotEmpty()) {
            try {
                val kiwoomStockInterestRes = runBlocking { stockClient.stockInterest(
                    req = KiwoomStockInterestReq(
                        stk_cd = krItems.joinToString("|") { it.stkCd }
                    )
                ) }

                if (kiwoomStockInterestRes.return_code == 0) {
                    krItems.forEach { item ->
                        val matched = kiwoomStockInterestRes.atn_stk_infr?.find { it.stk_cd == item.stkCd }
                        item.curPrc = matched?.cur_prc
                        item.fluRt = matched?.flu_rt
                        item.preSig = matched?.pred_pre_sig
                    }
                }
            } catch (e: Exception) {
                log.error { "국내 관심종목 시세 조회 실패 : ${e.message}" }
            }
        }

        usItems.forEach { item ->

            val quote = getUsQuote(stexTp = item.stexTp!!, stkCd = item.stkCd)
            item.curPrc = quote?.cur_prc
            item.fluRt = quote?.flu_rt
            item.preSig = quote?.pred_pre_sig
        }

        if (usItems.isNotEmpty()) {
            runBlocking { usSessionBaseService.currentBases(usItems.map { it.stkCd }) }?.let { b ->
                usItems.forEach { item ->
                    item.sessionLabel = b.session.label
                    item.regularClose = b.closes[item.stkCd]
                }
            }
        }

        return watchlistItemRes
    }

    private fun getUsQuote(stexTp: String, stkCd: String): KiwoomUsStockInfoRes? {
        return try {
            usStockClient.usStockInfo(KiwoomUsStockInfoReq(stex_tp = stexTp, stk_cd = stkCd))
        } catch (e: Exception) {
            log.error { "미국 관심종목 시세 조회 실패 : stkCd=$stkCd, ${e.message}" }
            null
        }
    }

    fun addItem(memberId: Long, groupId: Long, req: AddItemReq): WatchlistItemRes {
        val group = groupRepository.findById(groupId).orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }
        require(!itemRepository.existsByGroupIdAndStkCd(groupId, req.stkCd)) { "이미 추가된 종목입니다." }

        val nextOrder = itemRepository.countByGroupId(groupId)
        val item = itemRepository.save(
            WatchlistItem(
                groupId = groupId,
                stkCd = req.stkCd,
                stkNm = req.stkNm,
                stexTp = req.stexTp,
                displayOrder = nextOrder
            )
        )
        val displayNm = if (item.stexTp != null && usEtfService.isEtf(item.stkCd)) item.stkCd else item.stkNm
        return WatchlistItemRes(item.id, item.stkCd, displayNm, item.stexTp)
    }

    fun removeItem(memberId: Long, groupId: Long, itemId: Long) {
        val group = groupRepository.findById(groupId).orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }

        itemRepository.deleteById(itemId)
    }

    fun reorderItems(memberId: Long, groupId: Long, req: ReorderItemsReq) {
        val group = groupRepository.findById(groupId).orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }

        req.orderedIds.forEachIndexed { index, itemId ->
            val item = itemRepository.findById(itemId).orElseThrow { IllegalArgumentException("종목을 찾을 수 없습니다.") }
            item.displayOrder = index
        }
    }

    fun streamItems(memberId: Long, groupId: Long) {
        val group = groupRepository.findById(groupId).orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }

        val items = itemRepository.findByGroupIdOrderByDisplayOrderAsc(groupId)
        val (usItems, krItems) = items.partition { it.stexTp != null }

        kiwoomStreamClient.register(
            StreamEntry(
                market = StreamMarket.NXT,
                items = krItems.map { it.stkCd },
                types = listOf("0B")
            ),
            StreamEntry(
                market = StreamMarket.US,
                items = usItems.map { KiwoomUsStreamItem(jmcode = it.stkCd, stex_tp = it.stexTp!!) },
                types = listOf("FE")
            )
        )
    }
}
