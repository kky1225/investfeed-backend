package com.example.investfeed.telegram.client

import com.example.investfeed.telegram.dto.TelegramBotCommand
import com.example.investfeed.telegram.dto.TelegramGetUpdatesReq
import com.example.investfeed.telegram.dto.TelegramMessage
import com.example.investfeed.telegram.dto.TelegramResponse
import com.example.investfeed.telegram.dto.TelegramSendMessageReq
import com.example.investfeed.telegram.dto.TelegramSetMyCommandsReq
import com.example.investfeed.telegram.dto.TelegramUpdate
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.concurrent.atomic.AtomicBoolean

@Component
class TelegramClient(
    @Qualifier("telegramWebClient")
    private val telegramWebClient: WebClient,
    @param:Value("\${telegram.bot-token:}")
    private val botToken: String,
    @param:Value("\${telegram.bot-username:}")
    val botUsername: String,
) {
    private val log = KotlinLogging.logger {}

    val enabled: Boolean get() = botToken.isNotBlank()

    sealed class SendResult {
        data object Sent : SendResult()
        data object Blocked : SendResult()
        data class Failed(val reason: String) : SendResult()
    }

    fun sendHtml(chatId: Long, html: String): SendResult = send(TelegramSendMessageReq(chat_id = chatId, text = html, parse_mode = "HTML"))
    fun sendText(chatId: Long, text: String): SendResult = send(TelegramSendMessageReq(chat_id = chatId, text = text))

    /*
     * 텔레그램 WebClient 에는 공통 필터가 없어 실패는 여기서 기록한다.
     * 알림은 회원마다 보내고 setMyCommands 는 실패하는 동안 폴링마다 다시 부르므로,
     * 장애가 이어지는 동안은 첫 실패만 error, 이후는 성공할 때까지 warn (실패 1건에 error 1건이 장애 1건에 error 1건이 되도록)
     */
    private val sendFailing = AtomicBoolean(false)
    private val commandsFailing = AtomicBoolean(false)

    private fun logFailure(failing: AtomicBoolean, message: String) {
        if (failing.compareAndSet(false, true)) log.error { "$message (복구될 때까지 이후 실패는 warn)" }
        else log.warn { message }
    }

    /** 실패 상태였다면 풀고 warn 으로 복구를 남긴다 (스케줄러 밖이라 info 를 쓰지 않음) */
    private fun markRecovered(failing: AtomicBoolean, api: String) {
        if (failing.compareAndSet(true, false)) log.warn { "telegram $api 복구" }
    }

    /** 연결 실패 예외 메시지에는 요청 URL(봇 토큰 포함)이 붙으므로 가린다 */
    private fun Throwable.safeMessage(): String? =
        message?.let { if (botToken.isBlank()) it else it.replace(botToken, "***") }?.take(120)

    private fun send(req: TelegramSendMessageReq): SendResult {
        if (!enabled) return SendResult.Failed("telegram.bot-token 미설정")
        return try {
            val res = telegramWebClient.post()
                .uri("/bot$botToken/sendMessage")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<TelegramResponse<TelegramMessage>>() {})
                .block()
            if (res?.ok == true) SendResult.Sent.also { markRecovered(sendFailing, "sendMessage") }
            else { logFailure(sendFailing, "telegram sendMessage ok=false: chat=${req.chat_id} ${res?.description}"); SendResult.Failed("ok=false ${res?.description}") }
        } catch (e: WebClientResponseException) {
            val body = runCatching { e.getResponseBodyAs(object : ParameterizedTypeReference<TelegramResponse<Any>>() {}) }.getOrNull()
            val description = body?.description ?: ""
            when {
                e.statusCode.value() == 403 -> SendResult.Blocked
                e.statusCode.value() == 400 && description.contains("chat not found", ignoreCase = true) -> SendResult.Blocked
                else -> {
                    logFailure(sendFailing, "telegram sendMessage 실패: status=${e.statusCode.value()} chat=${req.chat_id} $description")
                    SendResult.Failed("status=${e.statusCode.value()} $description")
                }
            }
        } catch (e: Exception) {
            logFailure(sendFailing, "telegram sendMessage 오류: chat=${req.chat_id} ${e.javaClass.simpleName}: ${e.safeMessage()}")
            SendResult.Failed(e.javaClass.simpleName)
        }
    }

    fun setMyCommands(commands: List<TelegramBotCommand>): Boolean {
        if (!enabled) return false
        return try {
            val res = telegramWebClient.post()
                .uri("/bot$botToken/setMyCommands")
                .bodyValue(TelegramSetMyCommandsReq(commands))
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<TelegramResponse<Boolean>>() {})
                .block()
            if (res?.ok == true) true.also { markRecovered(commandsFailing, "setMyCommands") }
            else { logFailure(commandsFailing, "telegram setMyCommands ok=false: ${res?.description}"); false }
        } catch (e: WebClientResponseException) {
            logFailure(commandsFailing, "telegram setMyCommands 실패: status=${e.statusCode.value()}")
            false
        } catch (e: Exception) {
            logFailure(commandsFailing, "telegram setMyCommands 오류: ${e.javaClass.simpleName}: ${e.safeMessage()}")
            false
        }
    }

    class GetUpdatesException(message: String) : RuntimeException(message)

    fun getUpdates(offset: Long?): List<TelegramUpdate> {
        if (!enabled) return emptyList()
        return try {
            telegramWebClient.post()
                .uri("/bot$botToken/getUpdates")
                .bodyValue(TelegramGetUpdatesReq(offset = offset))
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<TelegramResponse<List<TelegramUpdate>>>() {})
                .block()
                ?.takeIf { it.ok }?.result ?: emptyList()
        } catch (e: WebClientResponseException) {
            val description = runCatching { e.getResponseBodyAs(object : ParameterizedTypeReference<TelegramResponse<Any>>() {})?.description }.getOrNull() ?: ""
            throw GetUpdatesException("status=${e.statusCode.value()} $description")
        } catch (e: Exception) {
            throw GetUpdatesException("${e.javaClass.simpleName}: ${e.safeMessage()}")
        }
    }
}
