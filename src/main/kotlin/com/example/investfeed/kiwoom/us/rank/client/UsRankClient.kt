package com.example.investfeed.kiwoom.us.rank.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.us.rank.dto.req.*
import com.example.investfeed.kiwoom.us.rank.dto.res.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity

@Component
class UsRankClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}
    private final val RANK_URL = "/api/us/rkinfo"
    private final val STKINFO_URL = "/api/us/stkinfo"

    companion object {
        private const val MAX_RANK_SIZE = 100
    }

    @KiwoomToken
    fun usStockTradeValueList(
        req: KiwoomUsStockTradeValueListReq
    ): KiwoomUsStockTradeValueListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val list = mutableListOf<KiwoomUsStockTradeValueRes>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + RANK_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "usa20540")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus( { it.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomUsStockTradeValueListRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
                    throw UsStockTradeValueListException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.result_list?.forEach { list.add(it) }

                contYn = entity.headers.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers.getFirst("next-key") ?: ""

                if (contYn == "N" || list.size >= MAX_RANK_SIZE) {
                    break
                }
            }

            return KiwoomUsStockTradeValueListRes(
                return_code = returnCode,
                return_msg = returnMsg,
                result_list = list.take(MAX_RANK_SIZE)
            )
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: UsStockTradeValueListException) {
            throw e
        }catch (e: Exception) {
            log.warn { "usStockTradeValueList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun usStockTradeVolumeList(
        req: KiwoomUsStockTradeVolumeListReq
    ): KiwoomUsStockTradeVolumeListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val list = mutableListOf<KiwoomUsStockTradeVolumeRes>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + RANK_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "usa20530")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ it.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomUsStockTradeVolumeListRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
                    throw UsStockTradeVolumeListException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.result_list?.forEach { list.add(it) }

                contYn = entity.headers.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers.getFirst("next-key") ?: ""

                if (contYn == "N" || list.size >= MAX_RANK_SIZE) {
                    break
                }
            }

            return KiwoomUsStockTradeVolumeListRes(
                return_code = returnCode,
                return_msg = returnMsg,
                result_list = list.take(MAX_RANK_SIZE)
            )
        }catch(e: KiwoomApiException){
            throw e
        }catch(e: UsStockTradeVolumeListException){
            throw e
        }catch (e: Exception) {
            log.warn { "usStockTradeVolumeList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun usStockSurgeTradeVolumeList(
        req: KiwoomUsSurgeTradeVolumeListReq
    ): KiwoomUsSurgeTradeVolumeListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val list = mutableListOf<KiwoomUsSurgeTradeVolumeRes>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + STKINFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "usa20520")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ it.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomUsSurgeTradeVolumeListRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
                    throw UsSurgeTradeVolumeListException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.result_list?.forEach { list.add(it) }

                contYn = entity.headers.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers.getFirst("next-key") ?: ""

                if (contYn == "N" || list.size >= MAX_RANK_SIZE) {
                    break
                }
            }

            return KiwoomUsSurgeTradeVolumeListRes(
                return_code = returnCode,
                return_msg = returnMsg,
                result_list = list.take(MAX_RANK_SIZE)
            )
        }catch(e: KiwoomApiException){
            throw e
        }catch(e: UsSurgeTradeVolumeListException){
            throw e
        }catch (e: Exception) {
            log.warn { "usStockSurgeTradeVolumeList Error" }

            throw RuntimeException(e.message)
        }
    }
}
