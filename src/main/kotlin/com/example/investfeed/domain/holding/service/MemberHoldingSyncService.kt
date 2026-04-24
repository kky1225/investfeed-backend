package com.example.investfeed.domain.holding.service

import com.example.investfeed.domain.holding.entity.Broker
import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.entity.MemberHolding
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MemberHoldingSyncService(
    private val memberHoldingRepository: MemberHoldingRepository,
    private val brokerRepository: BrokerRepository,
    private val stockClient: StockClient,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val CHUNK_SIZE = 100
    }

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

    /**
     * 수동(BrokerType.MANUAL) 보유종목의 stkNm 을 키움 API 응답값과 비교해 다르면 UPDATE.
     * 자동 키움 보유종목은 HoldingSyncScheduler 가 처리하므로 제외.
     * 일 1회 InterestSyncScheduler 에서 호출.
     */
    @Transactional
    fun syncAllManualStkNm() {
        val manualBrokers = brokerRepository.findAllByType(BrokerType.MANUAL)
        if (manualBrokers.isEmpty()) return

        val allManualHoldings = manualBrokers.flatMap { broker ->
            memberHoldingRepository.findByBrokerId(broker.id)
        }
        if (allManualHoldings.isEmpty()) return

        val distinctStkCds = allManualHoldings.map { it.stkCd }.distinct()
        var updatedCount = 0
        var processedCount = 0

        distinctStkCds.chunked(CHUNK_SIZE).forEach { chunk ->
            try {
                val res = stockClient.stockInterest(
                    req = KiwoomStockInterestReq(stk_cd = chunk.joinToString("|"))
                )

                val responseMap = res.atn_stk_infr?.associateBy { it.stk_cd ?: "" } ?: emptyMap()

                allManualHoldings.filter { it.stkCd in chunk }.forEach { holding ->
                    processedCount++
                    val freshStkNm = responseMap[holding.stkCd]?.stk_nm
                    if (!freshStkNm.isNullOrBlank() && freshStkNm != holding.stkNm) {
                        log.info { "수동 보유종목 stkNm 갱신: stkCd=${holding.stkCd}, ${holding.stkNm} -> $freshStkNm" }
                        holding.stkNm = freshStkNm
                        holding.updatedAt = LocalDateTime.now()
                        updatedCount++
                    }
                }
            } catch (e: Exception) {
                log.error { "수동 보유종목 stkNm 동기화 청크 실패: chunk size=${chunk.size}, ${e.message}" }
            }
        }

        log.info { "수동 보유종목 stkNm 동기화 완료: 처리 ${processedCount}건, 갱신 ${updatedCount}건" }
    }
}
