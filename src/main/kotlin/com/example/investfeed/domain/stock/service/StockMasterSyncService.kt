package com.example.investfeed.domain.stock.service

import com.example.investfeed.domain.stock.dto.req.StockInfoListReq
import com.example.investfeed.domain.stock.entity.StockMaster
import com.example.investfeed.domain.stock.repository.StockMasterRepository
import com.example.investfeed.kiwoom.stock.client.StockClient
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StockMasterSyncService(
    private val stockClient: StockClient,
    private val stockMasterRepository: StockMasterRepository,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        // 0:코스피, 10:코스닥, 8:ETF, 60:ETN
        private val MARKET_TYPES = listOf("0", "10", "8", "60")
    }

    @Transactional
    fun syncAll(): Int {
        val masters = MARKET_TYPES
            .flatMap { mrktTp ->
                (stockClient.stockInfoList(StockInfoListReq(mrkt_tp = mrktTp)).list ?: emptyList())
                    .map { mrktTp to it }
            }
            .distinctBy { (_, item) -> item.code }
            .mapNotNull { (mrktTp, item) ->
                val code = item.code ?: return@mapNotNull null
                val name = item.name ?: return@mapNotNull null
                StockMaster(
                    stkCd = code,
                    stkNm = name,
                    mrktTp = mrktTp,
                    mrktNm = if (item.marketName == "거래소") "코스피" else item.marketName,
                    upNm = item.upName,
                )
            }

        stockMasterRepository.deleteAllInBatch()
        stockMasterRepository.saveAll(masters)

        return masters.size
    }
}
