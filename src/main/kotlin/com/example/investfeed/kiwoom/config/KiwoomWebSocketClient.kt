package com.example.investfeed.kiwoom.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.springframework.stereotype.Component
import java.net.URI

@Component
class KiwoomWebSocketClient: WebSocketClient(URI("wss://api.kiwoom.com:10000/api/dostk/websocket")) {
    private val log = KotlinLogging.logger {}

    private var accessToken: String? = null
    private var request: String? = null
    private var trnm: String? = null

    fun setAccessToken(accessToken: String) {
        this.accessToken = accessToken
    }

    private val handlerMap: MutableMap<String, (String) -> Unit> = mutableMapOf()

    override fun onOpen(p0: ServerHandshake?) {
        log.info { "WebSocket onOpen" }

        accessToken?.let {
            handlerMap["LOGIN"] = {
                log.info { "로그인 응답: $it" }

                if (request != null) {
                    send(request)
                }
            }

            send(
                jacksonObjectMapper().writeValueAsString(
                    LoginStreamReq(
                        token = it,
                    )
                )
            )
        }
    }

    override fun onMessage(message: String?) {
        if (message == null) return

        try {
            val rootNode = jacksonObjectMapper().readTree(message)
            val trnm = rootNode.get("trnm")?.asText() ?: return

            if(trnm == "PING") {
                send(message)
            }

            if(rootNode.has("return_code")) {
                if(rootNode.get("return_code")?.asInt() != 0) {
                    val return_msg = rootNode.get("return_msg")?.asText() ?: "Socket error"
                    log.error { "WebSocket onMessage : $return_msg" }
                    handlerMap.remove(trnm)
                    return
                }
            }

            handlerMap[trnm]?.invoke(message)

            if (trnm != "REAL") {
                handlerMap.remove(trnm)
            }
        } catch (e: Exception) {

        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        log.info { "WebSocket onClose : $reason" }
    }

    override fun onError(e: Exception?) {
        log.error { "WebSocket onError : $e" }
    }

    fun setRequest(
        request: String,
        trnm: String,
    ) {
        this.request = request
        this.trnm = trnm
    }

    fun sendRealTimeHandler(
        trnm: String,
        handler: (String) -> Unit
    ) {
        handlerMap[trnm] = handler
    }
}