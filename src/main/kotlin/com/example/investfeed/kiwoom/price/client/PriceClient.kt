package com.example.investfeed.kiwoom.price.client

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
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity

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
    fun stockTradeInfo(
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
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomStockTradeInfoRes>()
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
            log.warn { "stockTradeInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockSinglePriceList(
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
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomStockSinglePriceRes>()
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
            log.warn { "stockSinglePriceList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun goldPriceNow(
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
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomGoldPriceNowRes>()
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
            log.warn { "goldPriceNow Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun goldPriceNowMinute(
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
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomGoldPriceNowMinuteRes>()
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
            log.warn { "goldPriceNowMinute Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun investorTradeOpenMarket(
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
                    .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomInvestorTradeOpenMarketRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
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
        } catch (e: Exception) {
            log.warn { "investorTradeOpenMarket Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun investorTradeCloseMarket(
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

                Thread.sleep(120)
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
            log.warn { "investorTradeOpenMarket Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun programTrade(
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
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomProgramTradeRes>()
                .block()

            if (res?.return_code != 0) {
                throw ProgramTradeException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: ProgramTradeException) {
          throw e
        } catch (e: Exception) {
            log.warn { "programTrade Error" }

            throw RuntimeException(e.message)
        }
    }

    fun stockProgramTradeDay(
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
                    .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomStockProgramTradeDayRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
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

                Thread.sleep(100)

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
        } catch (e: Exception) {
            log.warn { "stockProgramTradeDay Error" }

            throw RuntimeException(e.message)
        }
    }

    fun indexProgramTradeDay(
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
                .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomIndexProgramTradeDayRes>()
                .block()

            if (res?.return_code != 0) {
                throw IndexProgramTradeDayException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: IndexProgramTradeDayException) {
            throw e
        } catch (e: Exception) {
            log.warn { "indexProgramTradeDay Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockProgramTradeMinute(
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
                    .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomStockProgramTradeMinuteRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
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

                Thread.sleep(100)
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
        } catch (e: Exception) {
            log.warn { "stockProgramTradeMinute Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun indexProgramTradeMinute(
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
                    .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomIndexProgramTradeMinuteRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
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

                Thread.sleep(100)
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
        } catch (e: Exception) {
            log.warn { "indexProgramTradeMinute Error" }

            throw RuntimeException(e.message)
        }
    }
}