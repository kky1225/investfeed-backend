package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import mu.KotlinLogging
import org.springframework.core.NestedExceptionUtils
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 외부 API 호출의 실패 여부와 응답 시간을 집계한다.
 *
 * rate-limit 대기가 응답 시간에 섞이지 않도록 RateLimitExchangeFilter 안쪽에,
 * 재시도로 성공한 호출이 실패로 잡히지 않도록 재시도 필터 바깥쪽에 등록한다.
 * 로그에 URL 을 남기지 않는다 (ECOS·FRED 는 URL 에 API 키가 들어 있다).
 */
class ApiCallMetricsFilter(
    private val provider: ApiProvider,
    private val counterService: ApiCallCounterService,
    private val isFailureBody: ((ByteArray) -> Boolean)? = null,
) : ExchangeFilterFunction {
    private val log = KotlinLogging.logger {}

    companion object {
        private val RETURN_CODE = Regex("\"return_code\"\\s*:\\s*(-?\\d+)")
        private val EMPTY_BODY = ByteArray(0)

        /** 키움은 업무 오류(한도 초과, 토큰 오류 등)도 HTTP 200 에 return_code 로 내려준다 */
        internal fun isKiwoomFailureBody(body: ByteArray): Boolean {
            val code = RETURN_CODE.find(String(body, Charsets.ISO_8859_1))?.groupValues?.get(1)?.toIntOrNull()
            return code != null && code != 0
        }
    }

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> = Mono.defer {
        val startedAt = System.nanoTime()
        next.exchange(request)
            .flatMap { response -> inspect(response, startedAt) }
            .doOnError { e ->
                val latencyMs = elapsedMs(startedAt)
                counterService.recordResult(provider, failed = true, latencyMs = latencyMs)
                val cause = NestedExceptionUtils.getMostSpecificCause(e)
                val apiId = request.headers().getFirst("api-id")?.let { ", api-id=$it" } ?: ""
                log.error { "${provider.label} API 호출 실패$apiId, ${latencyMs}ms: ${cause.javaClass.simpleName}: ${cause.message}" }
            }
    }

    private fun inspect(response: ClientResponse, startedAt: Long): Mono<ClientResponse> {
        val inspectBody = isFailureBody
        if (inspectBody == null || response.statusCode().isError) {
            counterService.recordResult(provider, response.statusCode().isError, elapsedMs(startedAt))
            return Mono.just(response)
        }
        return response.bodyToMono(ByteArray::class.java)
            .defaultIfEmpty(EMPTY_BODY)
            .map { body ->
                counterService.recordResult(provider, inspectBody(body), elapsedMs(startedAt))
                response.mutate()
                    .body { _: Flux<DataBuffer> -> Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body)) }
                    .build()
            }
    }

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000
}
