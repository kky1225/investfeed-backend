package com.example.investfeed.global.config

import io.netty.channel.ChannelOption
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

/**
 * 외부 API 호출용 Reactor Netty HttpClient 공통 팩토리.
 *
 * 모든 WebClient 빈이 동일한 connect/response timeout + connection pool 설정을 갖도록 중앙 집중 관리한다.
 *
 * - connection pool 유휴 관리: 외부 서버(Naver, Kiwoom 등)가 먼저 idle 연결을 끊는 경우
 *   클라이언트가 stale 연결을 재사용하려다 "Connection prematurely closed BEFORE response"
 *   에러가 주기적으로 발생한다. maxIdleTime 을 일반적 서버 keep-alive(30~60s) 보다 짧게 잡고
 *   evictInBackground 로 주기 청소하여 근본 차단.
 */
object WebClientHttpClientFactory {

    private const val CONNECT_TIMEOUT_MS = 5_000
    private val RESPONSE_TIMEOUT: Duration = Duration.ofSeconds(60)

    private val connectionProvider: ConnectionProvider = ConnectionProvider.builder("investfeed-shared")
        .maxIdleTime(Duration.ofSeconds(20))          // 20초 유휴 → 자동 종료 (대부분 서버 keep-alive 이내)
        .maxLifeTime(Duration.ofMinutes(10))          // 10분 넘은 연결은 재활용 안 함
        .evictInBackground(Duration.ofSeconds(30))    // 30초마다 유휴 연결 청소
        .pendingAcquireTimeout(Duration.ofSeconds(5)) // 풀 고갈 시 5초 대기 후 실패
        .build()

    /**
     * connect timeout 5초, response(read) timeout 60초 + 유휴 연결 자동 정리 적용된 HttpClient 반환.
     */
    fun createDefaultHttpClient(): HttpClient {
        return HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
            .responseTimeout(RESPONSE_TIMEOUT)
    }
}
