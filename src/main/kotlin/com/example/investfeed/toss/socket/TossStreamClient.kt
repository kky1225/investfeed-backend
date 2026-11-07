package com.example.investfeed.toss.socket

import com.example.investfeed.domain.us.stock.service.UsSessionBaseService
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.toss.auth.service.TossAuthClient
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * 토스 미국 실시간 등록. 토스 연동 회원은 미국 실시간을 토스 체결로 받고, 미연동 회원은 키움 그대로다.
 * 토스 체결은 키움 미국 실시간 체결가(FE)와 같은 형식으로 보내 화면은 그대로 쓴다.
 *
 * 키움과 같은 규칙: 화면 이동에도 연결은 유지하고 다음 화면 등록이 구독을 덮어쓴다. 등록할 미국 종목이 없으면 구독만 비우고,
 * 화면 소켓이 5분간 없으면 정리 스케줄러가 연결을 끊는다. 토스 연결이 끊기면 화면 소켓을 닫아 화면이 다시 등록하게 한다.
 */
@Service
class TossStreamClient(
    private val tossSocketManager: TossSocketManager,
    private val tossAuthClient: TossAuthClient,
    private val usSessionBaseService: UsSessionBaseService,
    private val webSocketHandler: WebSocketHandler,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val TRADE_TYPE = "FE"   // 키움 미국주식 실시간 체결가와 같은 형식으로 보낸다
        private const val TRADE_NAME = "미국체결"
    }

    // 계정 → 종목별 전일대비 기준 종가
    private val closes = ConcurrentHashMap<String, Map<String, Double>>()

    /** 정리 스케줄러(매분): 화면 소켓이 5분간 없는 회원의 토스 연결을 끊는다 */
    fun releaseIdleConnections() {
        tossSocketManager.releaseIdleConnections().forEach { closes.remove(it) }
    }

    /**
     * 화면 실시간 등록 (키움 등록 전에 부른다). tickers: 미국 종목 티커.
     * 토스 연동 회원이면 토스로 구독하고 true 를 돌려준다. 이때 호출부는 미국 종목을 키움에 등록하지 않는다
     */
    fun register(tickers: List<String>): Boolean {
        val loginId = tossAuthClient.currentLoginIdOrNull() ?: return false
        if (!tossAuthClient.hasApiKey(loginId)) return false

        if (tickers.isEmpty()) {
            closes.remove(loginId)
            tossSocketManager.clear(loginId)
            return true
        }

        closes[loginId] = runBlocking { usSessionBaseService.streamCloses(tickers) }   // 구독 직후 오는 체결부터 새 기준으로 계산하도록 먼저 바꾼다
        tossSocketManager.subscribe(loginId, tickers.toSet(), ::onTrade, ::onLost)
        return true
    }

    /** 토스 소켓 수신 스레드에서 불린다 */
    private fun onTrade(loginId: String, symbol: String, price: String) {
        val prevClose = closes[loginId]?.get(symbol) ?: return
        val cur = price.toDoubleOrNull()?.takeIf { it > 0 } ?: return
        webSocketHandler.sendTo(setOf(loginId), tradeMessage(symbol, cur, prevClose))
    }

    private fun onLost(loginId: String) {
        closes.remove(loginId)
        webSocketHandler.requestReconnect(setOf(loginId))
    }

    /** 키움 실시간 체결가(FE)와 같은 모양: 10 현재가, 11 전일대비, 12 등락율, 25 전일대비기호 (부호 포함 문자열) */
    private fun tradeMessage(symbol: String, cur: Double, prevClose: Double): String {
        val diff = cur - prevClose
        val rate = diff / prevClose * 100
        val sign = when {
            diff > 0 -> "+"
            diff < 0 -> "-"
            else -> ""
        }
        val values = mapOf(
            "10" to sign + fmt(cur, 4),
            "11" to sign + fmt(abs(diff), 4),
            "12" to sign + fmt(abs(rate), 2),
            "25" to when { diff > 0 -> "2"; diff < 0 -> "5"; else -> "3" },
        )
        return objectMapper.writeValueAsString(
            mapOf("trnm" to "REAL", "data" to listOf(mapOf("type" to TRADE_TYPE, "name" to TRADE_NAME, "item" to symbol, "values" to values)))
        )
    }

    private fun fmt(v: Double, scale: Int): String = String.format(Locale.US, "%.${scale}f", v)
}
