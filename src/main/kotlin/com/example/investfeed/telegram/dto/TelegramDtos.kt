package com.example.investfeed.telegram.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramResponse<T>(
    val ok: Boolean,
    val result: T? = null,
    val description: String? = null,
    val error_code: Int? = null,
    val parameters: TelegramResponseParameters? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramResponseParameters(
    val retry_after: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramUpdate(
    val update_id: Long,
    val message: TelegramMessage? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramMessage(
    val message_id: Long,
    val chat: TelegramChat,
    val text: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramChat(
    val id: Long,
    val type: String? = null,   // private / group / supergroup / channel
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TelegramSendMessageReq(
    val chat_id: Long,
    val text: String,
    val parse_mode: String? = null,   // "HTML" 또는 null(평문)
    val disable_web_page_preview: Boolean = true,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TelegramGetUpdatesReq(
    val offset: Long? = null,
    val timeout: Int = 0,
    val allowed_updates: List<String> = listOf("message"),
)

/** setMyCommands 항목. command 는 "/" 없이 소문자·숫자·밑줄 1~32자, description 3~256자 */
data class TelegramBotCommand(
    val command: String,
    val description: String,
)

data class TelegramSetMyCommandsReq(
    val commands: List<TelegramBotCommand>,
)
