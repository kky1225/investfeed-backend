package com.example.investfeed.kiwoom.sect.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.sect.dto.req.*
import com.example.investfeed.kiwoom.sect.dto.res.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity

@Service
class SectClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}
    private final val SECT_URL = "/api/dostk/sect"

    @KiwoomToken
    fun sectInvestor(
        req: KiwoomSectInvestorReq
    ): KiwoomSectInvestorRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + SECT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10051")
                .bodyValue(req)
                .retrieve()
                .onStatus({ t -> t.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .bodyToMono<KiwoomSectInvestorRes>()
                .block()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=sectInvestor, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw SectInvestorException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectInvestorException) {
            throw e
        }catch (e: Exception) {
            log.warn { "sectInvestor Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectPriceNow(
        req: KiwoomSectPriceNowReq
    ): KiwoomSectPriceNowRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + SECT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20001")
                .bodyValue(req)
                .exchangeToMono { res ->
                    if (res.statusCode().isError) {
                        throw KiwoomApiException()
                    }

                    res.bodyToMono<KiwoomSectPriceNowRes>()
                }
                .block()

            log.info { "sectNowPriceRes $res" }

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=sectPriceNow, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw SectPriceNowException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: SectPriceNowException) {
            throw e
        } catch (e: Exception) {
            log.warn { "sectNowPrice Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectPrice(
        req: KiwoomSectPriceReq
    ): KiwoomSectPriceRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val inds_stkpc = mutableListOf<KiwoomSectPrice>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while(true) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + SECT_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "ka20002")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ t -> t.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                    .toEntity<KiwoomSectPriceRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
                    log.error { "키움 API 응답 오류: api=sectPrice, return_code=${entity?.body?.return_code}, return_msg=${entity?.body?.return_msg}, req=$req" }
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
            log.warn { "sectPrice Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectIndexList(
        req: KiwoomSectIndexReq
    ): KiwoomSectIndexRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + SECT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20003")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .bodyToMono<KiwoomSectIndexRes>()
                .block()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=sectIndexList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw SectIndexListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectIndexListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "sectIndexList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectIndexDailyList(
        req: KiwoomSectIndexDailyReq
    ): KiwoomSectIndexDailyRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + SECT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka20009")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .bodyToMono<KiwoomSectIndexDailyRes>()
                .block()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=sectIndexDailyList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw SectIndexDailyListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectIndexDailyListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "sectIndexDailyList Error" }

            throw RuntimeException(e.message)
        }
    }
}