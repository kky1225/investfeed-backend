package com.example.investfeed.domain.cryptointerest.service

import com.example.investfeed.domain.cryptointerest.dto.req.*
import com.example.investfeed.domain.cryptointerest.dto.res.CryptoInterestGroupRes
import com.example.investfeed.domain.cryptointerest.dto.res.CryptoInterestItemRes
import com.example.investfeed.domain.cryptointerest.entity.CryptoInterestGroup
import com.example.investfeed.domain.cryptointerest.entity.CryptoInterestItem
import com.example.investfeed.domain.cryptointerest.repository.CryptoInterestGroupRepository
import com.example.investfeed.domain.cryptointerest.repository.CryptoInterestItemRepository
import com.example.investfeed.upbit.ticker.client.TickerClient
import com.example.investfeed.upbit.websocket.client.CryptoStreamClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CryptoInterestService(
    private val groupRepository: CryptoInterestGroupRepository,
    private val itemRepository: CryptoInterestItemRepository,
    private val tickerClient: TickerClient,
    private val cryptoStreamClient: CryptoStreamClient,
) {

    @Transactional(readOnly = true)
    fun getGroups(memberId: Long): List<CryptoInterestGroupRes> {
        return groupRepository.findByMemberIdOrderByDisplayOrderAsc(memberId)
            .map { CryptoInterestGroupRes(it.id, it.groupNm, it.displayOrder) }
    }

    fun createGroup(memberId: Long, req: CreateCryptoGroupReq): CryptoInterestGroupRes {
        val nextOrder = groupRepository.countByMemberId(memberId)
        val group = groupRepository.save(
            CryptoInterestGroup(
                memberId = memberId,
                groupNm = req.groupNm,
                displayOrder = nextOrder
            )
        )
        return CryptoInterestGroupRes(group.id, group.groupNm, group.displayOrder)
    }

    fun updateGroup(memberId: Long, groupId: Long, req: UpdateCryptoGroupReq) {
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

    fun reorderGroups(memberId: Long, req: ReorderCryptoGroupsReq) {
        req.orderedIds.forEachIndexed { index, groupId ->
            val group = groupRepository.findById(groupId)
                .orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
            require(group.memberId == memberId) { "접근 권한이 없습니다." }
            group.displayOrder = index
        }
    }

    @Transactional(readOnly = true)
    fun getItems(memberId: Long, groupId: Long): List<CryptoInterestItemRes> {
        val group = groupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }

        val items = itemRepository.findByGroupIdOrderByDisplayOrderAsc(groupId)
            .map { CryptoInterestItemRes(it.id, it.market, it.koreanName) }

        if (items.isEmpty()) return items

        val markets = items.joinToString(",") { it.market }
        val tickers = tickerClient.getTickers(markets)
        items.forEach { item ->
            val ticker = tickers.find { it.market == item.market }
            item.tradePrice = ticker?.trade_price
            item.signedChangeRate = ticker?.signed_change_rate
            item.change = ticker?.change
        }

        return items
    }

    fun addItem(memberId: Long, groupId: Long, req: AddCryptoItemReq): CryptoInterestItemRes {
        val group = groupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }
        require(!itemRepository.existsByGroupIdAndMarket(groupId, req.market)) { "이미 추가된 코인입니다." }

        val nextOrder = itemRepository.countByGroupId(groupId)
        val item = itemRepository.save(
            CryptoInterestItem(
                groupId = groupId,
                market = req.market,
                koreanName = req.koreanName,
                displayOrder = nextOrder
            )
        )
        return CryptoInterestItemRes(item.id, item.market, item.koreanName)
    }

    fun removeItem(memberId: Long, groupId: Long, itemId: Long) {
        val group = groupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }
        itemRepository.deleteById(itemId)
    }

    fun reorderItems(memberId: Long, groupId: Long, req: ReorderCryptoItemsReq) {
        val group = groupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }

        req.orderedIds.forEachIndexed { index, itemId ->
            val item = itemRepository.findById(itemId)
                .orElseThrow { IllegalArgumentException("코인을 찾을 수 없습니다.") }
            item.displayOrder = index
        }
    }

    fun streamItems(memberId: Long, groupId: Long) {
        val group = groupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }

        val items = itemRepository.findByGroupIdOrderByDisplayOrderAsc(groupId)
        val markets = items.map { it.market }
        if (markets.isNotEmpty()) {
            cryptoStreamClient.cryptoListStream(markets)
        }
    }
}
