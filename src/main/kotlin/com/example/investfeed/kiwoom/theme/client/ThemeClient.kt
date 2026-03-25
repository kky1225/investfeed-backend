package com.example.investfeed.kiwoom.theme.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.exception.KiwoomApiException
import com.example.investfeed.kiwoom.exception.ThemeGroupListException
import com.example.investfeed.kiwoom.exception.ThemeGroupStockListException
import com.example.investfeed.kiwoom.theme.dto.req.KiwoomThemeGroupReq
import com.example.investfeed.kiwoom.theme.dto.req.KiwoomThemeGroupStockReq
import com.example.investfeed.kiwoom.theme.dto.res.KiwoomThemeGroup
import com.example.investfeed.kiwoom.theme.dto.res.KiwoomThemeGroupRes
import com.example.investfeed.kiwoom.theme.dto.res.KiwoomThemeGroupStockRes
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity

@Service
class ThemeClient(
    @param:Value("\${kiwoom.default-url}")
    private val DEFAULT_URL: String,
    private val webClient: WebClient,
    private val authClient: AuthClient
) {
    private val log = KotlinLogging.logger {}
    private final val THEME_URL = "/api/dostk/thme"

    @KiwoomToken
    fun themeGroupList(
        req: KiwoomThemeGroupReq
    ): KiwoomThemeGroupRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val thema_grp = mutableListOf<KiwoomThemeGroup>()
            var contYn = "N"
            var nextKey = ""
            var returnCode = 0
            var returnMsg = ""

            while(true) {
                val entity = webClient.post()
                    .uri(DEFAULT_URL + THEME_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header("api-id", "ka90001")
                    .header("cont-yn", contYn)
                    .header("next-key", nextKey)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus({ it.isError }, { throw KiwoomApiException() })
                    .toEntity<KiwoomThemeGroupRes>()
                    .block()

                if (entity?.body?.return_code != 0) {
                    throw ThemeGroupListException()
                }

                entity.body?.let { returnCode = it.return_code }
                entity.body?.let { returnMsg = it.return_msg }

                entity.body?.thema_grp?.forEach { thema_grp.add(it) }

                contYn = entity.headers?.getFirst("cont-yn") ?: "N"
                nextKey = entity.headers?.getFirst("next-key") ?: ""

                if (contYn == "N") {
                    break;
                }
            }

            return KiwoomThemeGroupRes(
                return_code = returnCode,
                return_msg = returnMsg,
                thema_grp = thema_grp
            )
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: ThemeGroupListException) {
            throw e
        }catch (e: Exception) {
            log.error { "themeGroupList Error" }

            throw RuntimeException(e.message)
        }
    }

    @KiwoomToken
    fun themeGroupStockList(
        req: KiwoomThemeGroupStockReq
    ): KiwoomThemeGroupStockRes {
        val accessToken = authClient.getCurrentAccessToken()

        try {
            val res = webClient.post()
                .uri(DEFAULT_URL + THEME_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header("api-id", "ka90002")
                .bodyValue(req)
                .retrieve()
                .onStatus({ it.isError }, { throw KiwoomApiException() })
                .bodyToMono(KiwoomThemeGroupStockRes::class.java)
                .block()

            if(res?.return_code != 0) {
                throw ThemeGroupStockListException()
            }

            return res
        }catch(e: KiwoomApiException) {
            throw e
        }catch(e: ThemeGroupStockListException) {
            throw e
        }catch (e: Exception) {
            log.error { "themeGroupStockList Error" }

            throw RuntimeException(e.message)
        }
    }
}