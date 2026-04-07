package com.example.investfeed.domain.notification.service

import com.example.investfeed.domain.notification.dto.req.PriceTargetCreateReq
import com.example.investfeed.domain.notification.dto.res.PriceTargetRes
import com.example.investfeed.domain.notification.entity.PriceTarget
import com.example.investfeed.domain.notification.repository.PriceTargetRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PriceTargetService(
    private val priceTargetRepository: PriceTargetRepository,
) {

    @Transactional
    fun createPriceTarget(memberId: Long, req: PriceTargetCreateReq): PriceTargetRes {
        val count = priceTargetRepository.countByMemberId(memberId)
        if (count >= 20) {
            throw IllegalStateException("목표가 알림은 최대 20개까지 등록할 수 있습니다.")
        }

        if (req.targetPrice <= 0) {
            throw IllegalArgumentException("목표가는 0보다 커야 합니다.")
        }

        val entity = priceTargetRepository.save(
            PriceTarget(
                memberId = memberId,
                assetType = req.assetType,
                assetCode = req.assetCode,
                assetName = req.assetName,
                targetPrice = req.targetPrice,
                direction = req.direction,
            )
        )

        return PriceTargetRes.from(entity)
    }

    fun getPriceTargets(memberId: Long): List<PriceTargetRes> {
        return priceTargetRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
            .map { PriceTargetRes.from(it) }
    }

    @Transactional
    fun deletePriceTarget(memberId: Long, id: Long) {
        val entity = priceTargetRepository.findById(id)
            .orElseThrow { IllegalArgumentException("목표가 알림을 찾을 수 없습니다.") }
        if (entity.memberId != memberId) {
            throw IllegalStateException("권한이 없습니다.")
        }
        priceTargetRepository.delete(entity)
    }
}
