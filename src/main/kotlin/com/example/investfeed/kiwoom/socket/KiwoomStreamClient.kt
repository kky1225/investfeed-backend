package com.example.investfeed.kiwoom.socket

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.socket.dto.KiwoomStream
import com.example.investfeed.kiwoom.socket.dto.StreamEntry
import com.example.investfeed.kiwoom.socket.dto.StreamMarket
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class KiwoomStreamClient(
    private val socketManager: KiwoomSocketManager,
    private val authClient: AuthClient,
    private val holidayService: HolidayService,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun register(vararg entries: StreamEntry) {
        val openEntries = entries.filter { it.items.isNotEmpty() && isOpen(it.market) }

        if (openEntries.isEmpty()) return

        val groups = StreamMarket.entries.map { it.grpNo }.distinct().associateWith { grpNo ->
            openEntries
                .filter { it.market.grpNo == grpNo }
                .map { KiwoomStream(item = it.items, type = it.types) }
        }

        log.debug { "실시간 등록 $groups" }

        socketManager.register(
            owner = authClient.getCurrentSocketOwner(),
            groups = groups,
        )
    }

    fun registerPersistent(loginId: String, entry: StreamEntry) {
        if (entry.items.isEmpty() || !isOpen(entry.market)) return

        authClient.ensureAccessToken(loginId)
        socketManager.ensurePersistent(
            owner = authClient.getSocketOwner(loginId),
            stream = KiwoomStream(item = entry.items, type = entry.types),
        )
    }

    /** 삭제된 API 키의 소켓 연결과 저장된 토큰을 정리한다 */
    fun releaseKey(appKey: String) {
        socketManager.disconnect(authClient.keyId(appKey))
        authClient.evictAccessToken(appKey)
    }

    fun removeSubscriber(appKey: String, loginId: String) {
        socketManager.removeSubscriber(authClient.keyId(appKey), loginId)
    }

    private fun isOpen(market: StreamMarket): Boolean = when (market) {
        StreamMarket.US -> true
        StreamMarket.KRX -> !holidayService.isHoliday() && MarketTimeUtil.isKrxOpen()
        StreamMarket.NXT -> !holidayService.isHoliday() && MarketTimeUtil.isNxtOpen()
    }
}
