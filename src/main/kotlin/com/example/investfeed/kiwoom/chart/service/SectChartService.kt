package com.example.investfeed.kiwoom.chart.service

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartDayListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.sect.res.SectChartDayListRes
import com.example.investfeed.kiwoom.chart.dto.sect.res.SectChartMinuteListRes
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.SectChartDayListException
import com.example.investfeed.kiwoom.exception.SectChartMinuteListException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class SectChartService(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    @KiwoomToken
    fun sectChartMinuteList(
        req: SectChartMinuteListReq
    ): SectChartMinuteListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20005")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(SectChartMinuteListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw SectChartMinuteListException()
            }

            val today = res.inds_min_pole_qry?.get(0)?.cntr_tm?.substring(0, 8)

            res.inds_min_pole_qry = res.inds_min_pole_qry?.filter { it.cntr_tm?.startsWith(today ?: "") == true }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: SectChartMinuteListException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectChartMinuteList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectChartDayList(
        req: SectChartDayListReq
    ): SectChartDayListRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20006")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(SectChartDayListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw SectChartDayListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: SectChartDayListException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectChartDayList Error" }

            throw RuntimeException(e.message)
        }
    }
}