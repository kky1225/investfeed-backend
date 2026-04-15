package com.example.investfeed.global.config

import io.netty.channel.ChannelOption
import reactor.netty.http.client.HttpClient
import java.time.Duration

/**
 * 외부 API 호출용 Reactor Netty HttpClient 공통 팩토리.
 *
 * 모든 WebClient 빈이 동일한 connect/response timeout 을 갖도록 중앙 집중 관리한다.
 * 이 팩토리가 없으면 sleep/wake 또는 외부 API 장애 시 WebClient 호출이
 * macOS 기본 TCP 타임아웃(약 16분) 까지 블로킹되어 스케줄러 스레드를 잠식한다.
 */
object WebClientHttpClientFactory {

    private const val CONNECT_TIMEOUT_MS = 5_000
    private val RESPONSE_TIMEOUT: Duration = Duration.ofSeconds(60)

    /**
     * connect timeout 5초, response(read) timeout 60초가 걸린 HttpClient 반환.
     */
    fun createDefaultHttpClient(): HttpClient {
        return HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
            .responseTimeout(RESPONSE_TIMEOUT)
    }
}
