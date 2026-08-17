package com.example.investfeed.domain.holding.scheduler

import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.holding.entity.Broker
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.holding.service.MemberHoldingSyncService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.notification.service.NotificationService
import com.example.investfeed.domain.notification.service.NotificationSettingService
import com.example.investfeed.kiwoom.holding.client.HoldingClient
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import com.example.investfeed.domain.auth.entity.MemberApiKey
import com.example.investfeed.toss.account.client.TossAccountClient
import com.example.investfeed.toss.holding.TossSymbolMapper
import com.example.investfeed.toss.holding.client.TossHoldingClient
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class HoldingSyncScheduler(
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val brokerRepository: BrokerRepository,
    private val memberHoldingSyncService: MemberHoldingSyncService,
    private val holdingClient: HoldingClient,
    private val tossAccountClient: TossAccountClient,
    private val tossHoldingClient: TossHoldingClient,
    private val schedulerLogService: SchedulerLogService,
    private val notificationService: NotificationService,
    private val notificationSettingService: NotificationSettingService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val OTHER_FAILURE_THRESHOLD = 0.10  // 기타실패율 10% 초과 시 시스템 이슈 의심
    }

    @Scheduled(cron = SchedulerCron.HOLDING_SYNC, scheduler = "slowScheduler")
    fun syncAllHoldings() {
        schedulerLogService.execute(SchedulerName.HoldingSyncScheduler) {
            log.info { "보유종목 동기화 스케줄러 시작" }
            val start = System.currentTimeMillis()

            syncKiwoomHoldings()
            syncTossHoldings()

            log.info { "보유종목 동기화 스케줄러 완료 (${System.currentTimeMillis() - start}ms)" }
        }
    }

    private fun syncKiwoomHoldings() {
        val kiwoomBroker = brokerRepository.findByName("키움증권") ?: return
        val kiwoomApiKeys = memberApiKeyRepository.findAllByBrokerId(kiwoomBroker.id)
            .filter { it.member.role.code != "ADMIN" }

        var successCount = 0
        var authFailCount = 0
        var otherFailCount = 0

        kiwoomApiKeys.forEach { apiKey ->
            try {
                withMemberContext(apiKey.member.loginId) {
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
                        broker = kiwoomBroker
                    )

                    successCount++
                    log.info { "키움 보유종목 동기화 완료: ${apiKey.member.loginId} (${holdings.size}건)" }
                }
            } catch (e: Exception) {
                if (isAuthFailure(e)) {
                    authFailCount++
                    log.warn { "키움 보유종목 동기화 인증 실패: loginId=${apiKey.member.loginId}, apiKeyId=${apiKey.id}, ${e.javaClass.simpleName}: ${e.message}" }
                    sendAuthFailedNotification(apiKey, kiwoomBroker)
                } else {
                    otherFailCount++
                    log.warn { "키움 보유종목 동기화 실패(기타): loginId=${apiKey.member.loginId}, apiKeyId=${apiKey.id}, ${e.javaClass.simpleName}: ${e.message}" }
                }
            }
        }

        val total = kiwoomApiKeys.size
        log.info { "키움 보유종목 동기화 완료: 성공 $successCount / 인증실패 $authFailCount / 기타실패 $otherFailCount (총 $total)" }

        if (total > 0 && otherFailCount.toDouble() / total >= OTHER_FAILURE_THRESHOLD) {
            log.error { "키움 보유종목 동기화 기타실패율 임계치 초과 ($otherFailCount/$total) — 키움 API 장애 의심" }
        }
        if (total > 0 && authFailCount == total) {
            log.error { "키움 보유종목 동기화 전원 인증 실패 ($total 명) — 키움 API 응답 확인 권장" }
        }
    }

    private fun syncTossHoldings() {
        val tossBroker = brokerRepository.findByName("토스증권") ?: return
        val tossApiKeys = memberApiKeyRepository.findAllByBrokerId(tossBroker.id)
            .filter { it.member.role.code != "ADMIN" }

        var successCount = 0
        var failCount = 0

        tossApiKeys.forEach { apiKey ->
            try {
                withMemberContext(apiKey.member.loginId) {
                    val accounts = tossAccountClient.getAccounts()
                    val accountSeq = (accounts.firstOrNull { it.accountType == "BROKERAGE" } ?: accounts.firstOrNull())?.accountSeq
                    val items = accountSeq?.let { tossHoldingClient.getHoldings(it)?.items } ?: emptyList()

                    val holdings = items.mapNotNull { item ->
                        val symbol = item.symbol ?: return@mapNotNull null
                        TossSymbolMapper.toStkCd(symbol, item.marketCountry) to (item.name ?: symbol)
                    }

                    memberHoldingSyncService.sync(
                        memberId = apiKey.member.id,
                        holdings = holdings,
                        broker = tossBroker
                    )

                    successCount++
                    log.info { "토스 보유종목 동기화 완료: ${apiKey.member.loginId} (${holdings.size}건)" }
                }
            } catch (e: Exception) {
                failCount++
                log.warn { "토스 보유종목 동기화 실패: loginId=${apiKey.member.loginId}, apiKeyId=${apiKey.id}, ${e.javaClass.simpleName}: ${e.message}" }
            }
        }

        log.info { "토스 보유종목 동기화 완료: 성공 $successCount / 실패 $failCount (총 ${tossApiKeys.size})" }
    }

    private fun isAuthFailure(e: Throwable): Boolean {
        val msg = e.message.orEmpty()
        return "API Key를 찾을 수 없습니다" in msg || "access token 오류" in msg
    }

    private fun sendAuthFailedNotification(apiKey: MemberApiKey, broker: Broker) {
        try {
            val setting = notificationSettingService.getSettingByMemberId(apiKey.member.id)
            if (!setting.apiKeyEnabled) return

            notificationService.createApiKeyExpiryAlert(
                memberId = apiKey.member.id,
                apiKeyId = apiKey.id,
                brokerName = broker.name,
                direction = Direction.API_KEY_AUTH_FAILED,
                daysLeft = 0
            )
        } catch (e: Exception) {
            log.warn { "API Key 인증 실패 알림 발송 실패: loginId=${apiKey.member.loginId}, apiKeyId=${apiKey.id}, ${e.message}" }
        }
    }

    private inline fun withMemberContext(loginId: String, block: () -> Unit) {
        setSecurityContext(loginId)
        try {
            block()
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    private fun setSecurityContext(loginId: String) {
        val auth = UsernamePasswordAuthenticationToken(loginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
