package com.example.investfeed.kiwoom.price.client

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.price.dto.req.*
import com.example.investfeed.kiwoom.price.dto.res.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class PriceClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}

    private val PRICE_URL = "/api/dostk/mrkcond";

    @KiwoomToken
    suspend fun stockTradeInfo(
        req: KiwoomStockTradeInfoReq
    ): KiwoomStockTradeInfoRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + PRICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10006")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomStockTradeInfoRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=stockTradeInfo, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw StockTradeInfoException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: StockTradeInfoException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "stockTradeInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun stockSinglePriceList(
        req: KiwoomStockSinglePriceReq
    ): KiwoomStockSinglePriceRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + PRICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10087")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomStockSinglePriceRes>()

            if(res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=stockSinglePriceList, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw StockSinglePriceListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockSinglePriceListException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            log.warn { "stockSinglePriceList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun goldPriceNow(
        req: KiwoomGoldPriceNowReq
    ): KiwoomGoldPriceNowRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + PRICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50100")
                .bodyValue(req)
                .retrieve()
                .onStatus({ t -> t.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomGoldPriceNowRes>()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=goldPriceNow, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw GoldPriceNowException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldPriceNowException) {
            throw e;
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "goldPriceNow Error: ${e.message}" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun goldPriceNowMinute(
        req: KiwoomGoldPriceNowMinuteReq
    ): KiwoomGoldPriceNowMinuteRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + PRICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka50101")
                .bodyValue(req)
                .retrieve()
                .onStatus({ t -> t.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomGoldPriceNowMinuteRes>()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=goldPriceNowMinute, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw GoldPriceNowMinuteException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: GoldPriceNowMinuteException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "goldPriceNowMinute Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun investorTradeOpenMarket(
        req: KiwoomInvestorTradeOpenMarketReq
    ): KiwoomInvestorTradeOpenMarketRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val opmr_invsr_trde = mutableListOf<KiwoomInvestorTradeOpenMarketItemList>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + PRICE_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "ka10063")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ t -> t.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                    .toEntity<KiwoomInvestorTradeOpenMarketRes>().awaitSingleOrNull()

                if (entity?.body?.return_code != 0) {
                    log.error { "키움 API 응답 오류: api=investorTradeOpenMarket, return_code=${entity?.body?.return_code}, return_msg=${entity?.body?.return_msg}, req=$req" }
                    throw InvestorTradeOpenMarketException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.opmr_invsr_trde?.forEach { opmr_invsr_trde.add(it) }

                contYn = entity.headers.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers.getFirst("next-key") ?: ""

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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "investorTradeOpenMarket Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun investorTradeCloseMarket(
        req: KiwoomInvestorTradeCloseMarketReq
    ): KiwoomInvestorTradeCloseMarketRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val opaf_invsr_trde = mutableListOf<KiwoomInvestorTradeCloseMarketItemList>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + PRICE_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "ka10066")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ t -> t.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                    .toEntity<KiwoomInvestorTradeCloseMarketRes>().awaitSingleOrNull()

                if (entity?.body?.return_code != 0) {
                    log.error { "키움 API 응답 오류: api=investorTradeCloseMarket, return_code=${entity?.body?.return_code}, return_msg=${entity?.body?.return_msg}, req=$req" }
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "investorTradeOpenMarket Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun programTrade(
        req: KiwoomProgramTradeReq
    ): KiwoomProgramTradeRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + PRICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka90010")
                .bodyValue(req)
                .retrieve()
                .onStatus({ t -> t.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomProgramTradeRes>()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=programTrade, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw ProgramTradeException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: ProgramTradeException) {
          throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "programTrade Error" }

            throw RuntimeException(e.message)
        }
    }

    suspend fun stockProgramTradeDay(
        req: KiwoomStockProgramTradeDayReq
    ): KiwoomStockProgramTradeDayRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val stk_daly_prm_trde_trnsn = mutableListOf<KiwoomStockProgramTradeDay>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + PRICE_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "ka90013")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ t -> t.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                    .toEntity<KiwoomStockProgramTradeDayRes>().awaitSingleOrNull()

                if (entity?.body?.return_code != 0) {
                    log.error { "키움 API 응답 오류: api=stockProgramTradeDay, return_code=${entity?.body?.return_code}, return_msg=${entity?.body?.return_msg}, req=$req" }
                    throw StockProgramTradeDayException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.stk_daly_prm_trde_trnsn?.forEach { stk_daly_prm_trde_trnsn.add(it) }

                contYn = entity.headers.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers.getFirst("next-key") ?: ""

                if (contYn == "N") {
                    break
                }


                if (stk_daly_prm_trde_trnsn.size >= 100) {
                    break
                }
            }

            return KiwoomStockProgramTradeDayRes(
                return_code = returnCode,
                return_msg = returnMsg,
                stk_daly_prm_trde_trnsn = stk_daly_prm_trde_trnsn
            )
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: StockProgramTradeDayException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "stockProgramTradeDay Error" }

            throw RuntimeException(e.message)
        }
    }

    suspend fun indexProgramTradeDay(
        req: KiwoomIndexProgramTradeDayReq
    ): KiwoomIndexProgramTradeDayRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + PRICE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka90007")
                .bodyValue(req)
                .retrieve()
                .onStatus({ t -> t.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                .awaitBodyOrNull<KiwoomIndexProgramTradeDayRes>()

            if (res?.return_code != 0) {
                log.error { "키움 API 응답 오류: api=indexProgramTradeDay, return_code=${res?.return_code}, return_msg=${res?.return_msg}, req=$req" }
                throw IndexProgramTradeDayException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: IndexProgramTradeDayException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "indexProgramTradeDay Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun stockProgramTradeMinute(
        req: KiwoomStockProgramTradeMinuteReq
    ): KiwoomStockProgramTradeMinuteRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val stk_tm_prm_trde_trnsn = mutableListOf<KiwoomStockProgramTradeMinute>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + PRICE_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "ka90008")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ t -> t.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                    .toEntity<KiwoomStockProgramTradeMinuteRes>().awaitSingleOrNull()

                if (entity?.body?.return_code != 0) {
                    log.error { "키움 API 응답 오류: api=stockProgramTradeMinute, return_code=${entity?.body?.return_code}, return_msg=${entity?.body?.return_msg}, req=$req" }
                    throw StockProgramTradeMinuteException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.stk_tm_prm_trde_trnsn?.forEach { stk_tm_prm_trde_trnsn.add(it) }

                contYn = entity.headers.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers.getFirst("next-key") ?: ""

                if (contYn == "N") {
                    break
                }

            }

            return KiwoomStockProgramTradeMinuteRes(
                return_code = returnCode,
                return_msg = returnMsg,
                stk_tm_prm_trde_trnsn = stk_tm_prm_trde_trnsn
            )
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: StockProgramTradeMinuteException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "stockProgramTradeMinute Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    suspend fun indexProgramTradeMinute(
        req: KiwoomIndexProgramTradeMinuteReq
    ): KiwoomIndexProgramTradeMinuteRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val prm_trde_trnsn = mutableListOf<KiwoomIndexProgramTradeMinute>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + PRICE_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "ka90005")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ t -> t.isError }, { res -> res.logHttpError("키움"); throw KiwoomApiException() })
                    .toEntity<KiwoomIndexProgramTradeMinuteRes>().awaitSingleOrNull()

                if (entity?.body?.return_code != 0) {
                    log.error { "키움 API 응답 오류: api=indexProgramTradeMinute, return_code=${entity?.body?.return_code}, return_msg=${entity?.body?.return_msg}, req=$req" }
                    throw IndexProgramTradeMinuteException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.prm_trde_trnsn?.forEach { prm_trde_trnsn.add(it) }

                contYn = entity.headers.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers.getFirst("next-key") ?: ""

                if (contYn == "N") {
                    break
                }

            }

            return KiwoomIndexProgramTradeMinuteRes(
                return_code = returnCode,
                return_msg = returnMsg,
                prm_trde_trnsn = prm_trde_trnsn
            )
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: IndexProgramTradeMinuteException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "indexProgramTradeMinute Error" }

            throw RuntimeException(e.message)
        }
    }
}