package com.example.investfeed.domain.interest.service

import com.example.investfeed.domain.interest.repository.InterestItemRepository
import com.example.investfeed.domain.us.stock.service.UsStockInfoService
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InterestSyncService(
    private val interestItemRepository: InterestItemRepository,
    private val stockClient: StockClient,
    private val usStockInfoService: UsStockInfoService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val CHUNK_SIZE = 100
    }

    @Transactional
    fun syncAllStkNm() {
        val allItems = interestItemRepository.findAll()
        if (allItems.isEmpty()) return

        val (usItems, krItems) = allItems.partition { it.stexTp != null }
        val distinctStkCds = krItems.map { it.stkCd }.distinct()
        var updatedCount = 0
        var processedCount = 0

        distinctStkCds.chunked(CHUNK_SIZE).forEach { chunk ->
            try {
                val res = stockClient.stockInterest(
                    req = KiwoomStockInterestReq(stk_cd = chunk.joinToString("|"))
                )

                val responseMap = res.atn_stk_infr?.associateBy { it.stk_cd ?: "" } ?: emptyMap()

                krItems.filter { it.stkCd in chunk }.forEach { item ->
                    processedCount++
                    val freshStkNm = responseMap[item.stkCd]?.stk_nm
                    if (!freshStkNm.isNullOrBlank() && freshStkNm != item.stkNm) {
                        log.info { "관심종목 stkNm 갱신: stkCd=${item.stkCd}, ${item.stkNm} -> $freshStkNm" }
                        item.stkNm = freshStkNm
                        updatedCount++
                    }
                }
            } catch (e: Exception) {
                log.warn { "관심종목 stkNm 동기화 청크 실패: chunk size=${chunk.size}, ${e.message}" }
            }
        }

        usItems.forEach { item ->
            try {
                processedCount++
                val freshStkNm = usStockInfoService.getStkNm(stexTp = item.stexTp!!, stkCd = item.stkCd)
                if (!freshStkNm.isNullOrBlank() && freshStkNm != item.stkNm) {
                    log.info { "미국 관심종목 stkNm 갱신: stkCd=${item.stkCd}, ${item.stkNm} -> $freshStkNm" }
                    item.stkNm = freshStkNm
                    updatedCount++
                }
            } catch (e: Exception) {
                log.warn { "미국 관심종목 stkNm 동기화 실패: stkCd=${item.stkCd}, ${e.message}" }
            }
        }

        log.info { "관심종목 stkNm 동기화 완료: 처리 ${processedCount}건, 갱신 ${updatedCount}건" }
    }
}
