package com.example.investfeed.kiwoom.config

import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class WebSocketHandler: TextWebSocketHandler() {
    private val log = KotlinLogging.logger {}

    private val sessions = mutableSetOf<WebSocketSession>()

    override fun afterConnectionEstablished(
        session: WebSocketSession
    ) {
        log.info { "afterConnectionEstablished: ${session.id}" }
        sessions.add(session)
    }

    override fun handleTextMessage(
        session: WebSocketSession,
        message: TextMessage
    ) {
        log.info { "handleTextMessage: ${message.payload}" }
        session.sendMessage(TextMessage("서버 응답: ${message.payload}"))
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        println("afterConnectionClosed: ${session.id}")
        sessions.remove(session)
    }

    fun broadcast(message: String) {
        for (session in sessions) {
            if (session.isOpen) {
                session.sendMessage(TextMessage(message))
            }
        }
    }
}