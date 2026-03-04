package com.example.investfeed.kiwoom.price.client

import com.example.investfeed.kiwoom.price.dto.req.KiwoomStockSinglePriceReq
import com.example.investfeed.kiwoom.price.dto.res.KiwoomStockSinglePriceRes
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.price.dto.req.KiwoomGoldPriceNowMinuteReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomGoldPriceNowReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomInvestorTradeCloseMarketReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomInvestorTradeOpenMarketReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomProgramTradeReq
import com.example.investfeed.kiwoom.price.dto.res.KiwoomGoldPriceNowMinuteRes
import com.example.investfeed.kiwoom.price.dto.res.KiwoomGoldPriceNowRes
import com.example.investfeed.kiwoom.price.dto.req.KiwoomStockTradeInfoReq
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeCloseMarketItemList
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeCloseMarketRes
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeOpenMarketItemList
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeOpenMarketRes
import com.example.investfeed.kiwoom.price.dto.res.KiwoomProgramTradeRes
import com.example.investfeed.kiwoom.price.dto.res.KiwoomStockTradeInfoRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity

@Component
class PriceClient(
    private val webClient: WebClient,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = KotlinLogging.logger {}

    @Value("\${kiwoom.default-url}")
    private lateinit var DEFAULT_URL: String

    private val PRICE_URL = "/api/dostk/mrkcond";

    @KiwoomToken
    fun stockTradeInfo(
        req: KiwoomStockTradeInfoReq
    ): KiwoomStockTradeInfoRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + PRICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10006")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockTradeInfoRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockTradeInfoException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: StockTradeInfoException) {
            throw e
        } catch (e: Exception) {
            log.error { "stockTradeInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockSinglePriceList(
        req: KiwoomStockSinglePriceReq
    ): KiwoomStockSinglePriceRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + PRICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10087")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockSinglePriceRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockSinglePriceListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockSinglePriceListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockSinglePriceList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun goldPriceNow(
        req: KiwoomGoldPriceNowReq
    ): KiwoomGoldPriceNowRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + PRICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50100")
                .bodyValue(req)
                .retrieve()
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomGoldPriceNowRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw GoldPriceNowException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldPriceNowException) {
            throw e;
        } catch (e: Exception) {
            log.error { "goldPriceNow Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun goldPriceNowMinute(
        req: KiwoomGoldPriceNowMinuteReq
    ): KiwoomGoldPriceNowMinuteRes? {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + PRICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50101")
                .bodyValue(req)
                .retrieve()
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomGoldPriceNowMinuteRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw GoldPriceNowMinuteException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldPriceNowMinuteException) {
            throw e
        } catch (e: Exception) {
            log.error { "goldPriceNowMinute Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun investorTradeOpenMarket(
        req: KiwoomInvestorTradeOpenMarketReq
    ): KiwoomInvestorTradeOpenMarketRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val opmr_invsr_trde = mutableListOf<KiwoomInvestorTradeOpenMarketItemList>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = webClient.post()
                    .uri(DEFAULT_URL + PRICE_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "ka10063")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomInvestorTradeOpenMarketRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
                    throw InvestorTradeOpenMarketException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.opmr_invsr_trde?.forEach { opmr_invsr_trde.add(it) }

                contYn = entity.headers?.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers?.getFirst("next-key") ?: ""

                if (contYn == "N") {
                    break
                }
            }

            return KiwoomInvestorTradeOpenMarketRes(
                return_code = returnCode,
                return_msg = returnMsg,
                opmr_invsr_trde = opmr_invsr_trde
            )
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: InvestorTradeOpenMarketException) {
            throw e
        } catch (e: Exception) {
            log.error { "investorTradeOpenMarket Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun investorTradeCloseMarket(
        req: KiwoomInvestorTradeCloseMarketReq
    ): KiwoomInvestorTradeCloseMarketRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val opaf_invsr_trde = mutableListOf<KiwoomInvestorTradeCloseMarketItemList>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = webClient.post()
                    .uri(DEFAULT_URL + PRICE_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "ka10066")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomInvestorTradeCloseMarketRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
                    throw InvestorTradeCloseMarketException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.opaf_invsr_trde?.forEach { opaf_invsr_trde.add(it) }

                contYn = entity.headers.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers.getFirst("next-key") ?: ""

                if (contYn == "N") {
                    break
                }

                Thread.sleep(80)
            }

            return KiwoomInvestorTradeCloseMarketRes(
                return_code = returnCode,
                return_msg = returnMsg,
                opaf_invsr_trde = opaf_invsr_trde
            )
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: InvestorTradeCloseMarketException) {
            throw e
        } catch (e: Exception) {
            log.error { "investorTradeOpenMarket Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun programTrade(
        req: KiwoomProgramTradeReq
    ): KiwoomProgramTradeRes {
        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + PRICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka90010")
                .bodyValue(req)
                .retrieve()
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomProgramTradeRes::class.java)
                .block()

            if (res?.return_code != 0) {
                throw GoldPriceNowMinuteException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: ProgramTradeException) {
          throw e
        } catch (e: Exception) {
            log.error { "programTrade Error" }

            throw RuntimeException(e.message)
        }
    }
}