package com.example.investfeed.ecos.client

import com.example.investfeed.ecos.dto.res.EcosStatRes
import com.example.investfeed.ecos.exception.EcosApiException
import com.example.investfeed.ecos.exception.EcosStatisticsException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class EcosClient(
    @Qualifier("ecosWebClient")
    private val ecosWebClient: WebClient,
    @param:Value("\${ecos.api-key}")
    private val apiKey: String,
) {
    private val log = KotlinLogging.logger {}

    /**
     * ECOS 통계 조회
     * @param tableCode 통계표코드
     * @param frequency 주기 (A:년, Q:분기, M:월, D:일)
     * @param startDate 시작일 (YYYYMMDD 또는 YYYY 등)
     * @param endDate 종료일
     * @param itemCode 항목코드 (선택)
     */
    fun getStatistics(
        tableCode: String,
        frequency: String,
        startDate: String,
        endDate: String,
        itemCode: String = "0",
        maxRows: Int = 100,
    ): EcosStatRes {
        try {
            val uri = "/api/StatisticSearch/$apiKey/json/kr/1/$maxRows/$tableCode/$frequency/$startDate/$endDate/$itemCode"
            val res = ecosWebClient.get()
                .uri(uri)
                .retrieve()
                .onStatus({ it.isError }, { throw EcosApiException() })
                .bodyToMono<EcosStatRes>()
                .block()

            if (res?.statisticSearch == null) {
                throw EcosStatisticsException()
            }

            log.info { "ECOS getStatistics: tableCode=$tableCode, frequency=$frequency" }

            return res
        } catch (e: EcosApiException) {
            throw e
        } catch (e: EcosStatisticsException) {
            throw e
        } catch (e: Exception) {
            log.error { "getStatistics Error: ${e.message}" }

            throw RuntimeException(e.message)
        }
    }
}
