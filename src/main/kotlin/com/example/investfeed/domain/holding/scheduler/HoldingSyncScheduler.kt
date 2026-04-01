package com.example.investfeed.domain.holding.scheduler

import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.holding.service.MemberHoldingSyncService
import com.example.investfeed.kiwoom.holding.client.HoldingClient
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class HoldingSyncScheduler(
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val memberHoldingSyncService: MemberHoldingSyncService,
    private val holdingClient: HoldingClient,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 0 * * *")
    fun syncAllHoldings() {
        log.info { "보유종목 동기화 스케줄러 시작" }
        val start = System.currentTimeMillis()

        val kiwoomApiKeys = memberApiKeyRepository.findAllByProvider("KIWOOM")

        kiwoomApiKeys.forEach { apiKey ->
            try {
                setSecurityContext(apiKey.member.loginId)

                val res = holdingClient.holdingList(
                    req = KiwoomHoldingReq(qry_tp = "2", dmst_stex_tp = "NXT")
                )

                val holdings = res?.acnt_evlt_remn_indv_tot?.map { stock ->
                    val stkCd = (stock.stk_cd?.removePrefix("A") ?: "") + "_AL"
                    val stkNm = stock.stk_nm ?: ""
                    stkCd to stkNm
                } ?: emptyList()

                memberHoldingSyncService.sync(
                    memberId = apiKey.member.id,
                    holdings = holdings,
                    provider = "KIWOOM"
                )

                log.info { "보유종목 동기화 완료: ${apiKey.member.loginId} (${holdings.size}건)" }
            } catch (e: Exception) {
                log.error { "보유종목 동기화 실패: ${apiKey.member.loginId} - ${e.message}" }
            } finally {
                SecurityContextHolder.clearContext()
            }
        }

        log.info { "보유종목 동기화 스케줄러 완료: ${System.currentTimeMillis() - start}ms" }
    }

    private fun setSecurityContext(loginId: String) {
        val auth = UsernamePasswordAuthenticationToken(loginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
