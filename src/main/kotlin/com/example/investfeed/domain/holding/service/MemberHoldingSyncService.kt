package com.example.investfeed.domain.holding.service

import com.example.investfeed.domain.holding.entity.Broker
import com.example.investfeed.domain.holding.entity.MemberHolding
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MemberHoldingSyncService(
    private val memberHoldingRepository: MemberHoldingRepository,
) {
    @Transactional
    fun sync(memberId: Long, holdings: List<Pair<String, String>>, broker: Broker) {
        val existing = memberHoldingRepository.findByMemberIdAndBrokerIdOrderByDisplayOrderAsc(memberId, broker.id)
        val existingMap = existing.associateBy { it.stkCd }
        val incomingStkCds = holdings.map { it.first }.toSet()

        // 매도된 종목 삭제
        existing.filter { it.stkCd !in incomingStkCds }.forEach { memberHoldingRepository.delete(it) }

        // 새로 편입된 종목 추가 (마지막 순서로)
        val maxOrder = existing.maxOfOrNull { it.displayOrder } ?: -1
        var nextOrder = maxOrder + 1

        holdings.forEach { (stkCd, stkNm) ->
            val existingHolding = existingMap[stkCd]
            if (existingHolding != null) {
                existingHolding.stkNm = stkNm
                existingHolding.updatedAt = LocalDateTime.now()
            } else {
                memberHoldingRepository.save(
                    MemberHolding(
                        memberId = memberId,
                        stkCd = stkCd,
                        stkNm = stkNm,
                        broker = broker,
                        displayOrder = nextOrder++,
                        updatedAt = LocalDateTime.now()
                    )
                )
            }
        }
    }
}
