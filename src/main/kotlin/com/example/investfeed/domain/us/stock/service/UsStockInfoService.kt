package com.example.investfeed.domain.us.stock.service

import com.example.investfeed.domain.us.stock.dto.res.UsStockSearchItem
import com.example.investfeed.kiwoom.us.stock.client.UsStockClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoListReq
import com.example.investfeed.kiwoom.us.stock.dto.res.KiwoomUsStockInfoListItem
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class UsStockInfoService(
    private val usStockClient: UsStockClient,
) {
    private val log = KotlinLogging.logger {}

    @Volatile
    private var cachedList: List<KiwoomUsStockInfoListItem> = emptyList()
    @Volatile
    private var loadedAt: LocalDateTime? = null
    private val cacheTtl: Duration = Duration.ofHours(24)
    private val loadLock = Any()

    fun searchUsStocks(
        keyword: String
    ): List<UsStockSearchItem> {
        val list = getCachedList()

        return list
            .filter {
                it.stk_nm?.contains(keyword, ignoreCase = true) == true
                        || it.stk_enm?.contains(keyword, ignoreCase = true) == true
                        || it.stk_cd?.contains(keyword, ignoreCase = true) == true
            }
            .distinctBy { "${it.stex_tp}|${it.stk_cd}" }
            .mapNotNull {
                val stkCd = it.stk_cd ?: return@mapNotNull null
                val stexTp = it.stex_tp ?: return@mapNotNull null
                UsStockSearchItem(
                    stkCd = stkCd,
                    stkNm = it.stk_nm ?: it.stk_enm ?: stkCd,
                    stexTp = stexTp,
                    marketName = it.mkgb ?: stexTp,
                )
            }
            .take(20)
    }

    fun getStkNm(
        stexTp: String,
        stkCd: String
    ): String? =
        getCachedList()
            .firstOrNull { it.stex_tp == stexTp && it.stk_cd == stkCd }
            ?.let { it.stk_nm ?: it.stk_enm }

    private fun getCachedList(): List<KiwoomUsStockInfoListItem> {
        val expired = loadedAt?.let { Duration.between(it, LocalDateTime.now()) > cacheTtl } ?: true

        if (expired) {
            synchronized(loadLock) {
                val stillExpired = loadedAt?.let { Duration.between(it, LocalDateTime.now()) > cacheTtl } ?: true

                if (stillExpired) {
                    try {
                        val res = usStockClient.usStockInfoList(KiwoomUsStockInfoListReq(stex_tp = "%"))
                        cachedList = res.list ?: emptyList()
                        loadedAt = LocalDateTime.now()
                    } catch (e: Exception) {
                        log.error { "usStockInfoList cache load Error : ${e.message}" }
                    }
                }
            }
        }

        return cachedList
    }
}
