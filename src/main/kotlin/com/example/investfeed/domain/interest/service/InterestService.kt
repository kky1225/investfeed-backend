package com.example.investfeed.domain.interest.service

import com.example.investfeed.domain.interest.dto.req.AddItemReq
import com.example.investfeed.domain.interest.dto.req.CreateGroupReq
import com.example.investfeed.domain.interest.dto.req.ReorderGroupsReq
import com.example.investfeed.domain.interest.dto.req.ReorderItemsReq
import com.example.investfeed.domain.interest.dto.req.UpdateGroupReq
import com.example.investfeed.domain.interest.dto.res.InterestGroupRes
import com.example.investfeed.domain.interest.dto.res.InterestItemRes
import com.example.investfeed.domain.interest.entity.InterestGroup
import com.example.investfeed.domain.interest.entity.InterestItem
import com.example.investfeed.domain.interest.repository.InterestGroupRepository
import com.example.investfeed.domain.interest.repository.InterestItemRepository
import com.example.investfeed.domain.stock.repository.StockMasterRepository
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.client.StockSocketClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStream
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStreamReq
import com.example.investfeed.kiwoom.us.stock.client.UsStockClient
import com.example.investfeed.kiwoom.us.stock.client.UsStockSocketClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoReq
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStream
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStreamItem
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStreamReq
import com.example.investfeed.kiwoom.us.stock.dto.res.KiwoomUsStockInfoRes
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class InterestService(
    private val groupRepository: InterestGroupRepository,
    private val itemRepository: InterestItemRepository,
    private val stockClient: StockClient,
    private val stockSocketClient: StockSocketClient,
    private val usStockClient: UsStockClient,
    private val usStockSocketClient: UsStockSocketClient,
    private val stockMasterRepository: StockMasterRepository,
) {
    private val log = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun getGroups(memberId: Long): List<InterestGroupRes> {
        return groupRepository.findByMemberIdOrderByDisplayOrderAsc(memberId)
            .map { InterestGroupRes(it.id, it.groupNm, it.displayOrder) }
    }

    fun createGroup(memberId: Long, req: CreateGroupReq): InterestGroupRes {
        val nextOrder = groupRepository.countByMemberId(memberId)
        val group = groupRepository.save(
            InterestGroup(
                memberId = memberId,
                groupNm = req.groupNm,
                displayOrder = nextOrder
            )
        )
        return InterestGroupRes(group.id, group.groupNm, group.displayOrder)
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
    fun getItems(memberId: Long, groupId: Long): List<InterestItemRes> {
        val group = groupRepository.findById(groupId).orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }

        val interestItemRes =  itemRepository.findByGroupIdOrderByDisplayOrderAsc(groupId).map { InterestItemRes(it.id, it.stkCd, it.stkNm, it.stexTp) }

        if (interestItemRes.isEmpty()) {
            return emptyList()
        }

        val (usItems, krItems) = interestItemRes.partition { it.stexTp != null }

        if (krItems.isNotEmpty()) {
            val marketMap = stockMasterRepository.findByStkCdIn(krItems.map { it.stkCd.substringBefore("_") }.distinct())
                .associate { it.stkCd to it.mrktNm }
            krItems.forEach { it.mrktNm = marketMap[it.stkCd.substringBefore("_")] }
        }

        if (krItems.isNotEmpty()) {
            try {
                val kiwoomStockInterestRes = stockClient.stockInterest(
                    req = KiwoomStockInterestReq(
                        stk_cd = krItems.joinToString("|") { it.stkCd }
                    )
                )

                if (kiwoomStockInterestRes.return_code == 0) {
                    krItems.forEach { interest ->
                        val matched = kiwoomStockInterestRes.atn_stk_infr?.find { it.stk_cd == interest.stkCd }
                        interest.curPrc = matched?.cur_prc
                        interest.fluRt = matched?.flu_rt
                        interest.preSig = matched?.pred_pre_sig
                    }
                }
            } catch (e: Exception) {
                log.error { "국내 관심종목 시세 조회 실패 : ${e.message}" }
            }
        }

        usItems.forEach { interest ->
            val quote = getUsQuote(stexTp = interest.stexTp!!, stkCd = interest.stkCd)
            interest.curPrc = quote?.cur_prc
            interest.fluRt = quote?.flu_rt
            interest.preSig = quote?.pred_pre_sig
        }

        return interestItemRes
    }

    private fun getUsQuote(stexTp: String, stkCd: String): KiwoomUsStockInfoRes? {
        return try {
            usStockClient.usStockInfo(KiwoomUsStockInfoReq(stex_tp = stexTp, stk_cd = stkCd))
        } catch (e: Exception) {
            log.error { "미국 관심종목 시세 조회 실패 : stkCd=$stkCd, ${e.message}" }
            null
        }
    }

    fun addItem(memberId: Long, groupId: Long, req: AddItemReq): InterestItemRes {
        val group = groupRepository.findById(groupId).orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }
        require(!itemRepository.existsByGroupIdAndStkCd(groupId, req.stkCd)) { "이미 추가된 종목입니다." }

        val nextOrder = itemRepository.countByGroupId(groupId)
        val item = itemRepository.save(
            InterestItem(
                groupId = groupId,
                stkCd = req.stkCd,
                stkNm = req.stkNm,
                stexTp = req.stexTp,
                displayOrder = nextOrder
            )
        )
        return InterestItemRes(item.id, item.stkCd, item.stkNm, item.stexTp)
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

        if (krItems.isNotEmpty()) {
            stockSocketClient.stockListStream(
                req = KiwoomStockStreamReq(
                    trnm = "REG",
                    grp_no = "0001",
                    refresh = "0",
                    data = listOf(
                        KiwoomStockStream(
                            item = krItems.map { it.stkCd },
                            type = listOf("0B")
                        )
                    )
                )
            )
        }

        if (usItems.isNotEmpty()) {
            usStockSocketClient.usStockListStream(
                req = KiwoomUsStockStreamReq(
                    trnm = "REG",
                    grp_no = "0001",
                    refresh = "0",
                    data = listOf(
                        KiwoomUsStockStream(
                            item = usItems.map {
                                KiwoomUsStockStreamItem(
                                    jmcode = it.stkCd,
                                    stex_tp = it.stexTp!!
                                )
                            },
                            type = listOf("FE")
                        )
                    )
                )
            )
        }
    }
}
