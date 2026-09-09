package com.example.investfeed.domain.us.stock.service

import com.example.investfeed.domain.us.stock.repository.UsStockMasterRepository
import org.springframework.stereotype.Service

@Service
class UsEtfService(
    private val usStockMasterRepository: UsStockMasterRepository,
) {
    fun isEtf(stkCd: String): Boolean = etfTickers(listOf(stkCd)).isNotEmpty()

    fun etfTickers(stkCds: Collection<String>): Set<String> {
        val targets = stkCds.filter { it.isNotBlank() }.distinct()
        if (targets.isEmpty()) return emptySet()

        return usStockMasterRepository.findByStkCdIn(targets)
            .filter { it.isEtf == "Y" }
            .map { it.stkCd }
            .toSet()
    }

    fun displayName(stkCd: String?, stkNm: String?, etfTickers: Set<String>): String? =
        if (stkCd != null && stkCd in etfTickers) stkCd else stkNm
}
