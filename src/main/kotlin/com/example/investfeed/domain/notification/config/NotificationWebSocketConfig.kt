package com.example.investfeed.domain.notification.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
class NotificationWebSocketConfig(
    private val notificationWebSocketHandler: NotificationWebSocketHandler,
    private val notificationHandshakeInterceptor: NotificationHandshakeInterceptor,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notification")
            .addInterceptors(notificationHandshakeInterceptor)
            .setAllowedOrigins("*")
    }
}
