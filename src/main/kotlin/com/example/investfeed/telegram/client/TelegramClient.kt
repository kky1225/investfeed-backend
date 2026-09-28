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

    private fun send(req: TelegramSendMessageReq): SendResult {
        if (!enabled) return SendResult.Failed("telegram.bot-token 미설정")
        return try {
            val res = telegramWebClient.post()
                .uri("/bot$botToken/sendMessage")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<TelegramResponse<TelegramMessage>>() {})
                .block()
            if (res?.ok == true) SendResult.Sent
            else { log.error { "telegram sendMessage ok=false: chat=${req.chat_id} ${res?.description}" }; SendResult.Failed("ok=false ${res?.description}") }
        } catch (e: WebClientResponseException) {
            val body = runCatching { e.getResponseBodyAs(object : ParameterizedTypeReference<TelegramResponse<Any>>() {}) }.getOrNull()
            val description = body?.description ?: ""
            when {
                e.statusCode.value() == 403 -> SendResult.Blocked
                e.statusCode.value() == 400 && description.contains("chat not found", ignoreCase = true) -> SendResult.Blocked
                else -> {
                    log.error { "telegram sendMessage 실패: status=${e.statusCode.value()} chat=${req.chat_id} $description" }
                    SendResult.Failed("status=${e.statusCode.value()} $description")
                }
            }
        } catch (e: Exception) {
            log.error { "telegram sendMessage 오류: chat=${req.chat_id} ${e.javaClass.simpleName}: ${e.message?.take(120)}" }
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
            if (res?.ok == true) true else { log.error { "telegram setMyCommands ok=false: ${res?.description}" }; false }
        } catch (e: WebClientResponseException) {
            log.error { "telegram setMyCommands 실패: status=${e.statusCode.value()}" }
            false
        } catch (e: Exception) {
            log.error { "telegram setMyCommands 오류: ${e.javaClass.simpleName}: ${e.message?.take(120)}" }
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
            throw GetUpdatesException("${e.javaClass.simpleName}: ${e.message?.take(120)}")
        }
    }
}
