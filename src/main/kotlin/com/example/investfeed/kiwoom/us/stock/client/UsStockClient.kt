package com.example.investfeed.kiwoom.us.stock.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.*
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoListReq
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoReq
import com.example.investfeed.kiwoom.us.stock.dto.res.KiwoomUsStockInfoListItem
import com.example.investfeed.kiwoom.us.stock.dto.res.KiwoomUsStockInfoListRes
import com.example.investfeed.kiwoom.us.stock.dto.res.KiwoomUsStockInfoRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity

@Component
class UsStockClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    @Qualifier("kiwoomWebClient")
    private val kiwoomWebClient: WebClient,
    private val authClient: AuthClient,
) {
    private val log = KotlinLogging.logger {}
    private final val MRKCOND_URL = "/api/us/mrkcond"
    private final val STKINFO_URL = "/api/us/stkinfo"

    @KiwoomToken
    fun usStockInfo(
        req: KiwoomUsStockInfoReq
    ): KiwoomUsStockInfoRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = kiwoomWebClient.post()
                .uri(DEFAULT_URL + MRKCOND_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "usa20100")
                .bodyValue(req)
                .retrieve()
                .onStatus( { it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomUsStockInfoRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw UsStockInfoException()
            }

            return res
        }catch (e: KiwoomApiException) {
            throw e
        }catch (e: UsStockInfoException) {
            throw e
        }catch (e: Exception) {
            log.warn { "usStockInfo Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun usStockInfoList(
        req: KiwoomUsStockInfoListReq
    ): KiwoomUsStockInfoListRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val list = mutableListOf<KiwoomUsStockInfoListItem>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while (true) {
                val entity = kiwoomWebClient.post()
                    .uri(DEFAULT_URL + STKINFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "usa10099")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus( { t -> t.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomUsStockInfoListRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
                    throw UsStockInfoListException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.list?.forEach { list.add(it) }

                contYn = entity.headers.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers.getFirst("next-key") ?: ""

                if (contYn == "N") {
                    break
                }

                Thread.sleep(1000)
            }

            return KiwoomUsStockInfoListRes(
                return_code = returnCode,
                return_msg = returnMsg,
                list = list
            )
        } catch (e: KiwoomApiException) {
            throw e
        } catch (e: UsStockInfoListException) {
            throw e
        } catch (e: Exception) {
            log.warn { "usStockInfoList Error" }

            throw RuntimeException(e.message)
        }
    }
}
