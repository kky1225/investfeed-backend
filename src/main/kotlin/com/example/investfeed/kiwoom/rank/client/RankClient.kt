package com.example.investfeed.kiwoom.rank.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.rank.dto.req.*
import com.example.investfeed.kiwoom.rank.dto.res.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity

@Component
class RankClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    private val webClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}
    private final val RANK_URL = "/api/dostk/rkinfo"

    @KiwoomToken
    fun stockTradeValueList(
        req: KiwoomStockTradeValueListReq
    ): KiwoomStockTradeValueListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + RANK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10032")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockTradeValueListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockTradeValueListException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: StockTradeValueListException) {
            throw e
        }catch (e: Exception) {
            log.error { "stockTradeValueList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockTradeVolumeList(
        req: KiwoomStockTradeVolumeListReq
    ): KiwoomStockTradeVolumeListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + RANK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10030")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomStockTradeVolumeListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockTradeVolumeListException()
            }

            return res
        }catch(e: KiwoomApiException){
            throw e
        }catch(e: StockTradeVolumeListException){
            throw e
        }catch (e: Exception) {
            log.error { "stockTradeVolumeList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun stockSurgeTradeVolumeList(
        req: KiwoomSurgeTradeVolumeListReq
    ): KiwoomSurgeTradeVolumeListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + RANK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10023")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomSurgeTradeVolumeListRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw StockSurgeTradeVolumeListException()
            }

            return res
        }catch(e: KiwoomApiException){
            throw e
        }catch(e: StockSurgeTradeVolumeListException){
            throw e
        }catch (e: Exception) {
            log.error { "stockSurgeTradeVolumeList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun investorTradeDaily(
        req: KiwoomInvestorTradeDailyReq
    ): KiwoomInvestorTradeDailyRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + RANK_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka10065")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomInvestorTradeDailyRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw InvestorTradeDailyException()
            }

            return res
        } catch(e: KiwoomApiException){
            throw e
        } catch(e: InvestorTradeDailyException){
            throw e
        } catch (e: Exception) {
            log.error { "investorTradeDaily Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun investorTrade(
        req: KiwoomInvestorTradeReq
    ): KiwoomInvestorTradeRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            var frgnr_orgn_trde_upper = mutableListOf<KiwoomInvestorTrade>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while(true) {
                val entity = webClient.post()
                    .uri(DEFAULT_URL + RANK_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "ka90009")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ it.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomInvestorTradeRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
                    throw InvestorTradeException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.frgnr_orgn_trde_upper?.forEach { frgnr_orgn_trde_upper.add(it) }

                contYn = entity.headers?.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers?.getFirst("next-key") ?: ""

                if (contYn == "N") {
                    break
                }
            }

            return KiwoomInvestorTradeRes(
                return_code = returnCode,
                return_msg = returnMsg,
                frgnr_orgn_trde_upper = frgnr_orgn_trde_upper
            )
        } catch(e: KiwoomApiException){
            throw e
        } catch(e: InvestorTradeException){
            throw e
        } catch (e: Exception) {
            log.error { "investorTrade Error" }

            throw RuntimeException(e.message)
        }
    }
}