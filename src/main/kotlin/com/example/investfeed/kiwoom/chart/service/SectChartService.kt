package com.example.investfeed.kiwoom.chart.service

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartDayListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartMonthListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartWeekListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartYearListReq
import com.example.investfeed.kiwoom.chart.dto.sect.res.KiwoomSectChartDayRes
import com.example.investfeed.kiwoom.chart.dto.sect.res.KiwoomSectChartMinuteRes
import com.example.investfeed.kiwoom.chart.dto.sect.res.KiwoomSectChartMonthRes
import com.example.investfeed.kiwoom.chart.dto.sect.res.KiwoomSectChartWeekRes
import com.example.investfeed.kiwoom.chart.dto.sect.res.KiwoomSectChartYearRes
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.SectChartDayListException
import com.example.investfeed.kiwoom.exception.SectChartMinuteListException
import com.example.investfeed.kiwoom.exception.SectChartMonthListException
import com.example.investfeed.kiwoom.exception.SectChartWeekListException
import com.example.investfeed.kiwoom.exception.SectChartYearListException
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
    ): KiwoomSectChartMinuteRes {
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
                .bodyToMono(KiwoomSectChartMinuteRes::class.java)
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
    ): KiwoomSectChartDayRes {
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
                .bodyToMono(KiwoomSectChartDayRes::class.java)
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

    @KiwoomToken
    fun sectChartWeekList(
        req: SectChartWeekListReq
    ): KiwoomSectChartWeekRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20007")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomSectChartWeekRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw SectChartWeekListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: SectChartWeekListException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectChartWeekList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectChartMonthList(
        req: SectChartMonthListReq
    ): KiwoomSectChartMonthRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20008")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomSectChartMonthRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw SectChartMonthListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: SectChartMonthListException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectChartMonthList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectChartYearList(
        req: SectChartYearListReq
    ): KiwoomSectChartYearRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri("$DEFAULT_URL/api/dostk/chart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20019")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomSectChartYearRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw SectChartYearListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: SectChartYearListException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectChartYearList Error" }

            throw RuntimeException(e.message)
        }
    }
}