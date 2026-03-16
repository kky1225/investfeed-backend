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
import org.springframework.web.reactive.function.client.toEntity

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

        try {
            val inds_stkpc = mutableListOf<KiwoomSectPrice>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while(true) {
                val entity = webClient.post()
                    .uri(DEFAULT_URL + SECT_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "ka20002")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ t -> t.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomSectPriceRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
                    throw SectPriceException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.inds_stkpc?.forEach { inds_stkpc.add(it) }

                contYn = entity.headers?.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers?.getFirst("next-key") ?: ""

                if (contYn == "N") {
                    break;
                }

                Thread.sleep(100)
            }

            return KiwoomSectPriceRes(
                return_code = returnCode,
                return_msg = returnMsg,
                inds_stkpc = inds_stkpc
            )
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
}