package com.example.investfeed.kiwoom.stock.client

import com.example.investfeed.domain.stock.dto.req.StockInfoListReq
import com.example.investfeed.domain.stock.dto.req.StockJumpListReq
import com.example.investfeed.domain.stock.dto.req.StockNewPriceListReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockTradeDailyListReq
import com.example.investfeed.domain.stock.dto.res.StockInfoListRes
import com.example.investfeed.domain.stock.dto.res.StockJumpListRes
import com.example.investfeed.domain.stock.dto.res.StockNewPriceListRes
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockTradeDailyRes
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomNewHighLowReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomSectCodeListReq
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomSectCodeListRes
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomDefaultStockInfoReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInfoReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInvestorReq
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockDefaultInfoRes
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockInfoRes
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomNewHighLowRes
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockInterestRes
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockInvestorRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class StockClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}
    private final val STOCK_URL = "/api/dostk/stkinfo"

    @KiwoomToken
    fun stockInfoList(
        req: StockInfoListReq
    ): StockInfoListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + STOCK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10099")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono<StockInfoListRes>()
                .block()

            if (res?.return_code != 0) {
                throw StockInfoListException()
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: StockInfoListException) {
            throw e
        } catch (e: Exception) {
            log.error { "stockInfoList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockDefaultInfo(
        req: KiwoomDefaultStockInfoReq
    ): KiwoomStockDefaultInfoRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + STOCK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10001")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomStockDefaultInfoRes>()
                .block()

            if(res?.return_code != 0) {
                throw StockDefaultInfoException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockDefaultInfoException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockInfo(
        req: KiwoomStockInfoReq
    ): KiwoomStockInfoRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + STOCK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10100")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomStockInfoRes>()
                .block()

            if(res?.return_code != 0) {
                throw StockInfoException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockInfoException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockInvestor(
        req: KiwoomStockInvestorReq
    ): KiwoomStockInvestorRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + STOCK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10059")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomStockInvestorRes>()
                .block()

            if(res?.return_code != 0) {
                throw StockInvestorException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockInvestorException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockInvestor Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockTradeDailyList(
        req: KiwoomStockTradeDailyListReq
    ): KiwoomStockTradeDailyRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + STOCK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10015")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomStockTradeDailyRes>()
                .block()

            if(res?.return_code != 0) {
                throw StockTradeDailyListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockTradeDailyListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockTradeDailyList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockJumpList(
        req: StockJumpListReq
    ): StockJumpListRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + STOCK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10019")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono<StockJumpListRes>()
                .block()

            if(res?.return_code != 0) {
                throw StockJumpListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockJumpListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockJumpList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockNewPriceList(
        req: StockNewPriceListReq
    ): StockNewPriceListRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + STOCK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10016")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono<StockNewPriceListRes>()
                .block()

            if(res?.return_code != 0) {
                throw StockNewPriceListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockNewPriceListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockNewPriceList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun sectCodeList(
        req: KiwoomSectCodeListReq
    ): KiwoomSectCodeListRes? {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + STOCK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10101")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomSectCodeListRes>()
                .block()

            if(res?.return_code != 0) {
                throw SectCodeListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: SectCodeListException) {
            throw e
        }catch (e: Exception) {
            log.error { "sectCodeList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockInterest(
        req: KiwoomStockInterestReq
    ): KiwoomStockInterestRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + STOCK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10095")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomStockInterestRes>()
                .block()

            if(res?.return_code != 0) {
                throw StockInterestException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockInterestException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockInterest Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun newHighLow(
        req: KiwoomNewHighLowReq
    ): KiwoomNewHighLowRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + STOCK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10016")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono<KiwoomNewHighLowRes>()
                .block()

            if (res?.return_code != 0) {
                throw RuntimeException("newHighLow Error: ${res?.return_msg}")
            }

            return res
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: Exception) {
            log.error { "newHighLow Error: ${e.message}" }
            throw RuntimeException(e.message)
        }
    }
}