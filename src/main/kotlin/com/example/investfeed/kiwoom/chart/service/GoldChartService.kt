package com.example.investfeed.kiwoom.chart.service

import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.gold.res.GoldChartMinuteListRes
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.GoldChartMinuteListException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class GoldChartService (
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    fun goldChartMinuteList (
        req: GoldChartMinuteListReq
    ): GoldChartMinuteListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50092")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(GoldChartMinuteListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw GoldChartMinuteListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldChartMinuteListException) {
            throw e
        } catch (e: Exception) {
            log.error { "goldChartMinuteList Error" }

            throw RuntimeException(e.message)
        }
    }
}