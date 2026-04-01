package com.example.investfeed.domain.holding.service

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
    fun sync(memberId: Long, holdings: List<Pair<String, String>>, provider: String) {
        memberHoldingRepository.deleteByMemberIdAndProvider(memberId, provider)

        val entities = holdings.map { (stkCd, stkNm) ->
            MemberHolding(
                memberId = memberId,
                stkCd = stkCd,
                stkNm = stkNm,
                provider = provider,
                updatedAt = LocalDateTime.now()
            )
        }

        memberHoldingRepository.saveAll(entities)
    }
}
