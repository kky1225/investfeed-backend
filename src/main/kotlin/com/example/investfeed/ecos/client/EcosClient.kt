package com.example.investfeed.ecos.client

import com.example.investfeed.ecos.dto.res.EcosStatRes
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
    ): EcosStatRes? {
        return try {
            val uri = "/api/StatisticSearch/$apiKey/json/kr/1/$maxRows/$tableCode/$frequency/$startDate/$endDate/$itemCode"
            val res = ecosWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono<EcosStatRes>()
                .block()

            log.info { "ECOS getStatistics: tableCode=$tableCode, frequency=$frequency" }
            res
        } catch (e: Exception) {
            log.error { "ECOS getStatistics Error: ${e.message}" }
            null
        }
    }
}
