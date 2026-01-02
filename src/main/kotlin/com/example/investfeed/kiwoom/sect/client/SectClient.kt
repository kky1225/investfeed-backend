package com.example.investfeed.kiwoom.sect.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.sect.dto.req.*
import com.example.investfeed.kiwoom.sect.dto.res.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalTime

@Service
class SectClient(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String
    private final val SECT_URL = "/api/dostk/sect"

    @KiwoomToken
    fun sectInvestor(
        req: KiwoomSectInvestorReq
    ): KiwoomSectInvestorRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + SECT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10051")
                .bodyValue(req)
                .retrieve()
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomSectInvestorRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw SectInvestorException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectInvestorException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectInvestor Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectPriceNow(
        req: KiwoomSectPriceNowReq
    ): KiwoomSectPriceNowRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + SECT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20001")
                .bodyValue(req)
                .exchangeToMono { res ->
                    if (res.statusCode().isError) {
                        throw KiwoomApiException()
                    }

                    res.bodyToMono(KiwoomSectPriceNowRes::class.java)
                }
                .block()

            log.info { "sectNowPriceRes $res" }

            if(res?.return_code != 0) {
                throw SectPriceNowException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: SectPriceNowException) {
            throw e
        } catch (e: Exception) {
            log.error { "sectNowPrice Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectPrice(
        req: KiwoomSectPriceReq
    ): KiwoomSectPriceRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

//        if(isMarketOpen()) {
//            SectSocketService.sectIndexListStream(
//                accessToken = accessToken,
//                req = SectIndexListStreamReq(
//                    trnm = "REG",
//                    grp_no = "0001",
//                    refresh = "0",
//                    data = listOf(
//                        SectIndexListStream(
//                            item = listOf("001", "101", "201"),
//                            type = listOf("0J")
//                        )
//                    )
//                )
//            )
//        }

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + SECT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20002")
                .bodyValue(req)
                .retrieve()
                .onStatus({ t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomSectPriceRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw SectPriceException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectPriceException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectPrice Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectIndexList(
        req: KiwoomSectIndexReq
    ): KiwoomSectIndexRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

//        if(isMarketOpen()) {
//            SectSocketService.sectIndexListStream(
//                accessToken = accessToken,
//                req = SectIndexListStreamReq(
//                    trnm = req.trnm,
//                    grp_no = req.grp_no,
//                    refresh = req.refresh,
//                    data = req.data
//                )
//            )
//        }

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + SECT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20003")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomSectIndexRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw SectIndexListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectIndexListException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectIndexList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectIndexDailyList(
        req: KiwoomSectIndexDailyReq
    ): KiwoomSectIndexDailyRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + SECT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20009")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomSectIndexDailyRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw SectIndexDailyListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectIndexDailyListException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectIndexDailyList Error" }

            throw RuntimeException(e.message)
        }
    }

    fun isMarketOpen(): Boolean {
        val now = LocalTime.now()

        return !now.isBefore(LocalTime.of(9, 0)) && !now.isAfter(LocalTime.of(15, 30))
    }
}