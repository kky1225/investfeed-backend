package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.entity.TelegramStatus
import com.example.investfeed.telegram.client.TelegramClient
import com.example.investfeed.telegram.dto.TelegramBotCommand
import com.example.investfeed.telegram.dto.TelegramUpdate
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class TelegramCommandService(
    private val telegramClient: TelegramClient,
    private val telegramLinkService: TelegramLinkService,
    private val telegramNotifier: TelegramNotifier,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        /** 봇 메뉴에 등록하는 명령 목록. 기동 시 [TelegramUpdatePoller] 가 setMyCommands 로 올린다 */
        val BOT_COMMANDS = listOf(
            TelegramBotCommand("start", "연결 (웹 비서 설정에서 받은 코드와 함께)"),
            TelegramBotCommand("status", "연결 상태 확인"),
            TelegramBotCommand("stop", "알림 일시중지 (재개는 웹)"),
            TelegramBotCommand("help", "사용할 수 있는 명령"),
        )
        const val REPLY_HELP = "사용할 수 있는 명령\n" +
            "/start {코드} – 웹 비서 설정에서 받은 코드로 연결\n" +
            "/status – 연결 상태\n" +
            "/stop – 알림 일시중지 (재개는 웹 비서 설정)\n" +
            "질문·설정 변경은 웹 비서에서 해 주세요."
        const val REPLY_LINKED = "연결되었습니다. 지수 급변·서킷브레이커·지표 발표·보유 종목 급등락 알림을 이 채팅으로 보냅니다.\n/status · /stop 을 쓸 수 있습니다."
        const val REPLY_BAD_CODE = "코드가 맞지 않거나 만료되었습니다. 웹 비서 설정에서 새 코드를 받아 주세요."
        const val REPLY_NOT_LINKED = "연결되지 않은 채팅입니다. 웹 비서 설정 → 텔레그램 연결에서 코드를 받아 /start {코드} 를 보내 주세요."
        const val REPLY_STOPPED = "발송을 일시중지했습니다. 재개는 웹 비서 설정에서 할 수 있습니다."
        const val REPLY_OTHER = "텔레그램은 알림 전용입니다.\n" + REPLY_HELP
        const val REPLY_SEND_BLOCKED = "현재 전체 발송이 중지되어 있습니다."
    }

    fun handle(update: TelegramUpdate) {
        val message = update.message ?: return
        if (message.chat.type != "private") return
        val text = message.text?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val chatId = message.chat.id

        val command = text.substringBefore(' ').substringBefore('@').lowercase()
        val arg = text.substringAfter(' ', "").trim()
        val reply = when (command) {
            "/start" -> start(chatId, arg)
            "/status" -> status(chatId)
            "/stop" -> if (telegramLinkService.pauseByChat(chatId)) REPLY_STOPPED else REPLY_NOT_LINKED
            "/help" -> REPLY_HELP
            else -> if (telegramLinkService.findByChatId(chatId) == null) REPLY_NOT_LINKED else REPLY_OTHER
        }
        val result = telegramClient.sendText(chatId, reply)
        log.info { "텔레그램 명령 처리: chat=$chatId cmd=$command result=${result::class.simpleName}" }   // 폴러(스케줄러) 경로라 성공 로그 허용. 응답 도달 여부 추적용
    }

    private fun start(chatId: Long, code: String): String {
        if (!code.matches(Regex("\\d{6}"))) {
            return if (telegramLinkService.findByChatId(chatId) == null) REPLY_NOT_LINKED else REPLY_OTHER
        }
        return if (telegramLinkService.link(code, chatId)) {
            log.info { "텔레그램 연결: chat=$chatId" }
            REPLY_LINKED
        } else REPLY_BAD_CODE
    }

    private fun status(chatId: Long): String {
        val setting = telegramLinkService.findByChatId(chatId) ?: return REPLY_NOT_LINKED
        val first = when (setting.telegramStatus) {
            TelegramStatus.ACTIVE -> "연결됨 · 발송 중"
            TelegramStatus.PAUSED -> "연결됨 · 일시중지 (재개는 웹 설정에서)"
            TelegramStatus.BLOCKED -> "연결됨 · 봇 차단 감지 (차단을 풀고 웹 설정에서 다시 켜 주세요)"
            TelegramStatus.NONE -> return REPLY_NOT_LINKED
        }
        return listOfNotNull(
            first,
            REPLY_SEND_BLOCKED.takeIf { telegramNotifier.isSendBlocked() },
            "알림 종류별 수신은 웹 비서 설정을 따릅니다.",
        ).joinToString("\n")
    }
}
