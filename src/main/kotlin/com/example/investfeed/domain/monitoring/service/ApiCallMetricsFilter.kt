package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import org.slf4j.MDC
import mu.KotlinLogging
import org.springframework.core.NestedExceptionUtils
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.util.UriUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URLEncoder

/**
 * 외부 API 호출의 실패 여부와 응답 시간을 집계한다.
 *
 * rate-limit 대기가 응답 시간에 섞이지 않도록 RateLimitExchangeFilter 안쪽에,
 * 재시도로 성공한 호출이 실패로 잡히지 않도록 재시도 필터 바깥쪽에 등록한다.
 *
 * 외부 API 호출 1건의 상세는 여기서만 debug 1줄로 남긴다 (클라이언트에서 호출 로그를 남기지 않음). FAST 스케줄러 안의 호출은 제외.
 * 쿼리 문자열은 남기지 않는다 (FRED·공공데이터는 쿼리에 키가 있다).
 * 경로에 키·토큰이 있는 곳(ECOS·텔레그램)은 [secrets] 로 실제 값을 받아, 경로는 남기고 그 값 자리만 [SECRET_PLACEHOLDER] 로 바꾼다.
 * 클라이언트가 주소를 어떻게 만들든(문자열 템플릿·URI 변수) 실제로 나가는 경로에서 값을 찾아 바꾸므로 키가 남지 않는다.
 */
class ApiCallMetricsFilter(
    private val provider: ApiProvider,
    private val counterService: ApiCallCounterService,
    private val isFailureBody: ((ByteArray) -> Boolean)? = null,
    secrets: List<String> = emptyList(),
) : ExchangeFilterFunction {
    private val log = KotlinLogging.logger {}

    /**
     * 경로에서 찾을 비밀 값의 모든 형태 (원래 값 + URL 인코딩된 값). 긴 것부터 바꿔야 일부만 바뀌어 조각이 남지 않는다.
     * 빈 값은 뺀다 (미설정 토큰 "" 를 바꾸면 모든 글자 사이에 자리 표시가 들어간다)
     */
    private val secretForms: List<String> = secrets
        .filter { it.isNotBlank() }
        .flatMap { secret ->
            listOf(
                secret,
                UriUtils.encodePath(secret, Charsets.UTF_8),        // 경로 템플릿으로 넣을 때 (':' 등은 그대로)
                UriUtils.encodePathSegment(secret, Charsets.UTF_8), // URI 변수로 넣을 때 ('/' 도 인코딩)
                URLEncoder.encode(secret, Charsets.UTF_8),          // 직접 인코딩해 넣을 때 (':' → %3A)
            )
        }
        .distinct()
        .sortedByDescending { it.length }

    companion object {
        const val SECRET_PLACEHOLDER = "{secret}"
        private val RETURN_CODE = Regex("\"return_code\"\\s*:\\s*(-?\\d+)")
        private val EMPTY_BODY = ByteArray(0)

        /**
         * 요청 attribute: true 면 연결 실패를 warn 으로만 남긴다 — error 는 호출 클래스가 묶어서 남기는 경우
         * (네이버 시장지표는 실행당 1건, 텔레그램은 장애당 1건)
         */
        const val CALLER_LOGS_ERROR_ATTR = "apiCallMetrics.callerLogsError"

        /** 키움은 업무 오류(한도 초과, 토큰 오류 등)도 HTTP 200 에 return_code 로 내려준다 */
        internal fun isKiwoomFailureBody(body: ByteArray): Boolean {
            val code = RETURN_CODE.find(String(body, Charsets.ISO_8859_1))?.groupValues?.get(1)?.toIntOrNull()
            return code != null && code != 0
        }
    }

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> = Mono.defer {
        val startedAt = System.nanoTime()
        // 구독은 호출한 스레드에서 일어나므로 여기서 MDC 를 읽는다 (응답은 reactor 스레드라 MDC 가 없음)
        val logSuccess = !isFastSchedulerCall()
        next.exchange(request)
            .flatMap { response -> inspect(response, startedAt) }
            .doOnNext { response ->
                if (logSuccess) {
                    log.debug { "${provider.label} ${request.method()} ${target(request)} ${response.statusCode().value()} ${elapsedMs(startedAt)}ms" }
                }
            }
            .doOnError { e ->
                val latencyMs = elapsedMs(startedAt)
                counterService.recordResult(provider, failed = true, latencyMs = latencyMs)
                val cause = NestedExceptionUtils.getMostSpecificCause(e)
                val apiId = request.headers().getFirst("api-id")?.let { ", api-id=$it" } ?: ""
                val message = "${provider.label} API 호출 실패$apiId, ${latencyMs}ms: ${cause.javaClass.simpleName}: ${cause.message}"
                if (request.attribute(CALLER_LOGS_ERROR_ATTR).orElse(false) == true) log.warn { message } else log.error { message }
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

    /**
     * FAST 스케줄러(매초·5초·매분) 안의 호출인지. 같은 호출을 하루 수만 번 반복하므로 성공 debug 를 남기지 않는다
     * (집계·실패 로그는 그대로). SchedulerLogService.execute 가 MDC 에 실행 중인 스케줄러 이름을 넣는다
     */
    private fun isFastSchedulerCall(): Boolean {
        val name = MDC.get(SchedulerLogService.MDC_SCHEDULER_NAME) ?: return false
        return SchedulerName.entries.firstOrNull { it.name == name }?.type == SchedulerType.FAST
    }

    /**
     * 호스트 + 경로(비밀 값은 자리 표시로) + 키움 api-id.
     * 인코딩된 %3a·%3A 를 모두 잡도록 대소문자를 가리지 않는다 (더 많이 가리는 쪽으로만 틀린다)
     */
    internal fun target(request: ClientRequest): String {
        val url = request.url()
        val path = secretForms.fold(url.rawPath ?: "") { acc, form -> acc.replace(form, SECRET_PLACEHOLDER, ignoreCase = true) }
        val apiId = request.headers().getFirst("api-id")?.let { " api-id=$it" } ?: ""
        return "${url.host}$path$apiId"
    }

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000
}
