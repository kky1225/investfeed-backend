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
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class InterestService(
    private val groupRepository: InterestGroupRepository,
    private val itemRepository: InterestItemRepository,
    private val stockClient: StockClient
) {

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
            val group = groupRepository.findById(groupId)
                .orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
            require(group.memberId == memberId) { "접근 권한이 없습니다." }
            group.displayOrder = index
        }
    }

    @Transactional(readOnly = true)
    fun getItems(memberId: Long, groupId: Long): List<InterestItemRes> {
        val group = groupRepository.findById(groupId).orElseThrow { IllegalArgumentException("그룹을 찾을 수 없습니다.") }
        require(group.memberId == memberId) { "접근 권한이 없습니다." }

        var stk_cd = ""

        var interestItemRes =  itemRepository.findByGroupIdOrderByDisplayOrderAsc(groupId).map { InterestItemRes(it.id, it.stkCd, it.stkNm) }
        interestItemRes.forEach {
            stk_cd = stk_cd + it.stkCd + "|"
        }

        val kiwoomStockInterestRes = stockClient.stockInterest(
            req = KiwoomStockInterestReq(
                stk_cd = stk_cd
            )
        )

        if (kiwoomStockInterestRes.return_code == 0) {
            interestItemRes.forEach { interest ->
                interest.curPrc = kiwoomStockInterestRes.atn_stk_infr?.find { it.stk_cd == interest.stkCd }?.cur_prc
                interest.fluRt = kiwoomStockInterestRes.atn_stk_infr?.find { it.stk_cd == interest.stkCd }?.flu_rt
                interest.preSig = kiwoomStockInterestRes.atn_stk_infr?.find { it.stk_cd == interest.stkCd }?.pred_pre_sig
            }
        }

        return interestItemRes
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
                displayOrder = nextOrder
            )
        )
        return InterestItemRes(item.id, item.stkCd, item.stkNm)
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
}
