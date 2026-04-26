package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.monitoring.dto.req.*
import com.example.investfeed.domain.monitoring.dto.res.*
import com.example.investfeed.domain.monitoring.entity.*
import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.repository.*
import com.example.investfeed.global.constant.RedisKeyPrefix
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.management.ManagementFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.management.ObjectName

@Service
class MonitoringService(
    private val schedulerLogRepository: SchedulerLogRepository,
    private val schedulerStatusRepository: SchedulerStatusRepository,
    private val schedulerConfigLogRepository: SchedulerConfigLogRepository,
    private val errorLogRepository: ErrorLogRepository,
    private val logAckHistoryRepository: LogAckHistoryRepository,
    private val memberRepository: MemberRepository,
    private val redisTemplate: StringRedisTemplate,
    private val entityManager: EntityManager,
    private val apiCallCounterService: ApiCallCounterService,
    @Qualifier("fastScheduler") private val fastScheduler: ThreadPoolTaskScheduler,
    @Qualifier("slowScheduler") private val slowScheduler: ThreadPoolTaskScheduler,
) {
    private val log = KotlinLogging.logger {}

    fun getStatuses(): List<SchedulerStatusRes> =
        schedulerStatusRepository.findAllByOrderBySchedulerNameAsc().map { SchedulerStatusRes.from(it, computeState(it)) }

    fun getCatalog(): List<SchedulerCatalogRes> =
        SchedulerName.entries.map { SchedulerCatalogRes.from(it) }

    fun getSchedulerOverview(req: SchedulerLogsReq): SchedulerOverviewRes =
        SchedulerOverviewRes(
            catalog = getCatalog(),
            statuses = getStatuses(),
            logs = getLogs(req),
            unackCount = getUnacknowledgedCount(),
        )

    fun getConfigLogsOverview(req: SchedulerConfigLogsReq): ConfigLogsOverviewRes =
        ConfigLogsOverviewRes(
            logs = getConfigLogs(req),
            unackCount = getUnacknowledgedCount(),
        )

    fun getRedisOverview(): RedisOverviewRes =
        RedisOverviewRes(
            redis = getRedisStats(),
            unackCount = getUnacknowledgedCount(),
        )

    fun getErrorLogsOverview(req: ErrorLogsReq): ErrorLogsOverviewRes =
        ErrorLogsOverviewRes(
            logs = getErrorLogs(req),
            unackCount = getUnacknowledgedCount(),
        )

    fun getApiCallsOverview(): ApiCallsOverviewRes =
        ApiCallsOverviewRes(
            stats = getApiCallStats(),
            unackCount = getUnacknowledgedCount(),
        )

    fun getSystemOverview(): SystemOverviewRes =
        SystemOverviewRes(
            system = getSystemStatus(),
            unackCount = getUnacknowledgedCount(),
        )

    // ─────────── 외부 API 호출 통계 ───────────

    fun getApiCallStats(): ApiCallStatsRes {
        val items = ApiProvider.entries.map { provider ->
            val recent = apiCallCounterService.getRecent7Days(provider)
            val today = recent.lastOrNull()?.second ?: 0L
            val ratio = provider.dailyLimit?.let { limit ->
                if (limit > 0) today.toDouble() / limit.toDouble() else null
            }
            ApiCallStatsItemRes(
                provider = provider.name,
                label = provider.label,
                todayCount = today,
                dailyLimit = provider.dailyLimit,
                usageRatio = ratio,
                recent7Days = recent.map { (date, count) -> DailyCallCount(date.toString(), count) },
            )
        }
        return ApiCallStatsRes(items)
    }

    /**
     * 스케줄러 상태 판정.
     * - PENDING: 한 번도 실행 안 됨
     * - STUCK: 시작했는데 안 끝나고 timeout_sec 초과
     * - FAILED: 마지막 실행이 실패
     * - WARNING: 마지막은 성공이지만 24h 내 FAILED 또는 INTERRUPTED 이력 있음
     * - SUCCESS: 정상
     */
    private fun computeState(s: SchedulerStatus): String {
        val now = LocalDateTime.now()

        if (s.lastSuccessAt == null && s.lastFailureAt == null) return "PENDING"

        val startedAt = s.lastStartedAt
        val finishedAt = s.lastFinishedAt
        val isStuck = startedAt != null &&
            (finishedAt == null || finishedAt.isBefore(startedAt)) &&
            Duration.between(startedAt, now).seconds > s.timeoutSec
        if (isStuck) return "STUCK"

        val lastFailureAt = s.lastFailureAt
        val lastSuccessAt = s.lastSuccessAt
        val lastFailureIsLatest = lastFailureAt != null &&
            (lastSuccessAt == null || lastFailureAt.isAfter(lastSuccessAt))
        if (lastFailureIsLatest) return "FAILED"

        val yesterday = now.minusHours(24)
        val hasRecentAnomaly = schedulerLogRepository.existsBySchedulerNameAndStatusInAndStartedAtAfterAndAcknowledgedFalse(
            s.schedulerName, listOf("FAILED", "INTERRUPTED"), yesterday
        )
        if (hasRecentAnomaly) return "WARNING"

        return "SUCCESS"
    }

    @Transactional
    fun updateTimeout(schedulerName: String, req: UpdateSchedulerTimeoutReq, changedBy: Long): SchedulerStatusRes {
        val status = schedulerStatusRepository.findById(schedulerName).orElseThrow {
            IllegalArgumentException("존재하지 않는 스케줄러: $schedulerName")
        }
        val oldValue = status.timeoutSec
        if (oldValue != req.timeoutSec) {
            // managed entity → dirty checking 으로 자동 UPDATE (save 불필요)
            status.timeoutSec = req.timeoutSec
            status.updatedAt = LocalDateTime.now()

            schedulerConfigLogRepository.save(
                SchedulerConfigLog(
                    schedulerName = schedulerName,
                    fieldName = "timeout_sec",
                    oldValue = oldValue.toString(),
                    newValue = req.timeoutSec.toString(),
                    changedBy = changedBy,
                    reason = req.reason,
                )
            )
        }
        return SchedulerStatusRes.from(status, computeState(status))
    }

    /**
     * 특정 실행 이력 확인 처리 또는 사유 수정.
     * - 최초: acknowledged=false → true, 이력에 ACKNOWLEDGE 기록
     * - 이미 확인됨: 사유만 교체, 이력에 EDIT_NOTE 기록
     */
    @Transactional
    fun acknowledgeLog(logId: Long, req: AcknowledgeLogReq, changedBy: Long): SchedulerLogRes {
        val logEntry = schedulerLogRepository.findById(logId).orElseThrow {
            IllegalArgumentException("존재하지 않는 로그: $logId")
        }
        val now = LocalDateTime.now()
        if (!logEntry.acknowledged) {
            // 최초 확인 — managed entity 라 dirty checking 으로 자동 UPDATE
            logEntry.acknowledged = true
            logEntry.acknowledgedBy = changedBy
            logEntry.acknowledgedAt = now
            logEntry.acknowledgeNote = req.note
            logAckHistoryRepository.save(LogAckHistory(
                sourceType = AckSourceType.SCHEDULER_LOG,
                sourceId = logId,
                action = AckAction.ACKNOWLEDGE,
                oldNote = null,
                newNote = req.note,
                actedBy = changedBy,
                actedAt = now,
            ))
        } else if (logEntry.acknowledgeNote != req.note) {
            // 사유 수정 — dirty checking 으로 자동 UPDATE
            val oldNote = logEntry.acknowledgeNote
            logEntry.acknowledgeNote = req.note
            logAckHistoryRepository.save(LogAckHistory(
                sourceType = AckSourceType.SCHEDULER_LOG,
                sourceId = logId,
                action = AckAction.EDIT_NOTE,
                oldNote = oldNote,
                newNote = req.note,
                actedBy = changedBy,
                actedAt = now,
            ))
        }
        val name = logEntry.acknowledgedBy?.let { resolveNicknames(setOf(it))[it] }
        return SchedulerLogRes.from(logEntry, name)
    }

    /**
     * 미확인 스케줄러 로그 일괄 확인 처리.
     * - SUCCESS 는 ack 대상이 아니므로 자동 제외
     * - note 비었으면 "일괄 확인" 디폴트
     * - LogAckHistory 에 BULK_ACKNOWLEDGE action 으로 기록
     */
    @Transactional
    fun bulkAcknowledgeSchedulerLogs(req: BulkAcknowledgeReq, changedBy: Long): BulkAcknowledgeRes {
        val appliedNote = req.note?.takeIf { it.isNotBlank() } ?: "일괄 확인"
        val ids = req.ids?.takeIf { it.isNotEmpty() }
        val targets = if (ids != null) {
            schedulerLogRepository.findByIdInAndAcknowledgedFalseAndStatusNot(ids, "SUCCESS")
        } else {
            schedulerLogRepository.findByAcknowledgedFalseAndStatusNot("SUCCESS")
        }
        val now = LocalDateTime.now()
        targets.forEach { entry ->
            entry.acknowledged = true
            entry.acknowledgedBy = changedBy
            entry.acknowledgedAt = now
            entry.acknowledgeNote = appliedNote
            logAckHistoryRepository.save(LogAckHistory(
                sourceType = AckSourceType.SCHEDULER_LOG,
                sourceId = entry.id,
                action = AckAction.BULK_ACKNOWLEDGE,
                oldNote = null,
                newNote = appliedNote,
                actedBy = changedBy,
                actedAt = now,
            ))
        }
        return BulkAcknowledgeRes(processedCount = targets.size, appliedNote = appliedNote)
    }

    /** 스케줄러 로그 확인 취소. 모든 ack 필드 리셋 + 이력에 CANCEL 기록. */
    @Transactional
    fun cancelAcknowledgeLog(logId: Long, changedBy: Long): SchedulerLogRes {
        val logEntry = schedulerLogRepository.findById(logId).orElseThrow {
            IllegalArgumentException("존재하지 않는 로그: $logId")
        }
        if (!logEntry.acknowledged) {
            return SchedulerLogRes.from(logEntry)
        }
        val oldNote = logEntry.acknowledgeNote
        // managed entity 라 dirty checking 으로 자동 UPDATE
        logEntry.acknowledged = false
        logEntry.acknowledgedBy = null
        logEntry.acknowledgedAt = null
        logEntry.acknowledgeNote = null
        logAckHistoryRepository.save(LogAckHistory(
            sourceType = AckSourceType.SCHEDULER_LOG,
            sourceId = logId,
            action = AckAction.CANCEL,
            oldNote = oldNote,
            newNote = null,
            actedBy = changedBy,
        ))
        return SchedulerLogRes.from(logEntry)
    }

    /**
     * 여러 member id 를 한 번에 nickname 매핑.
     * - null/0 id 는 "시스템 액션" 으로 간주하고 매핑 대상에서 제외
     * - 유효한 id 중 DB 에 존재하지 않는 것이 있으면 데이터 무결성 위반으로 예외 발생
     *   (nickname 은 Member 엔티티의 NOT NULL 필수 필드)
     */
    private fun resolveNicknames(ids: Set<Long>): Map<Long, String> {
        val valid = ids.filter { it > 0 }.toSet()
        if (valid.isEmpty()) return emptyMap()
        val members = memberRepository.findAllById(valid)
        if (members.size != valid.size) {
            val foundIds = members.map { it.id }.toSet()
            val missing = valid - foundIds
            throw IllegalStateException("존재하지 않는 회원 ID 참조: $missing")
        }
        return members.associate { it.id to it.nickname }
    }

    /** 로그 확인 이력 조회. 스케줄러/에러 공통. */
    fun getAckHistory(sourceType: AckSourceType, sourceId: Long): List<LogAckHistoryRes> {
        val list = logAckHistoryRepository.findBySourceTypeAndSourceIdOrderByActedAtDesc(sourceType, sourceId)
        val names = resolveNicknames(list.map { it.actedBy }.toSet())
        return list.map { LogAckHistoryRes.from(it, names[it.actedBy]) }
    }

    fun getConfigLogs(req: SchedulerConfigLogsReq): Page<SchedulerConfigLogRes> {
        val pageable = PageRequest.of(req.page, req.size)
        val pageResult = if (!req.schedulerName.isNullOrBlank()) {
            schedulerConfigLogRepository.findBySchedulerNameOrderByChangedAtDesc(req.schedulerName, pageable)
        } else {
            schedulerConfigLogRepository.findAllByOrderByChangedAtDesc(pageable)
        }
        // changedBy 를 모아서 한 번에 member 조회
        val memberIds = pageResult.content.map { it.changedBy }.toSet()
        val nameMap = if (memberIds.isEmpty()) emptyMap() else memberRepository.findAllById(memberIds).associate { it.id to it.nickname }
        return pageResult.map { SchedulerConfigLogRes.from(it, nameMap[it.changedBy]) }
    }

    fun getLogs(req: SchedulerLogsReq): Page<SchedulerLogRes> {
        val pageable = PageRequest.of(req.page, req.size, Sort.Direction.DESC, "startedAt")
        val spec = Specification<SchedulerLog> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            req.schedulerName?.takeIf { it.isNotBlank() }?.let {
                predicates.add(cb.equal(root.get<String>("schedulerName"), it))
            }
            req.status?.takeIf { it.isNotBlank() }?.let {
                predicates.add(cb.equal(root.get<String>("status"), it))
            }
            req.acknowledged?.let {
                predicates.add(cb.equal(root.get<Boolean>("acknowledged"), it))
                if (!it) predicates.add(cb.notEqual(root.get<String>("status"), "SUCCESS"))
            }
            req.fromDate?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), it.atStartOfDay()))
            }
            req.toDate?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("startedAt"), it.atTime(23, 59, 59, 999_999_999)))
            }
            req.messageKeyword?.takeIf { it.isNotBlank() }?.let {
                val pattern = "%${it.lowercase()}%"
                predicates.add(cb.like(cb.lower(root.get("errorMessage")), pattern))
            }
            cb.and(*predicates.toTypedArray())
        }
        val result = schedulerLogRepository.findAll(spec, pageable)
        val names = resolveNicknames(result.content.mapNotNull { it.acknowledgedBy }.toSet())
        return result.map { SchedulerLogRes.from(it, names[it.acknowledgedBy]) }
    }

    // ─────────── Redis ───────────

    fun getRedisStats(): RedisCacheRes {
        val result = RedisKeyPrefix.values().map { keyPrefix ->
            val keys = scanKeys("${keyPrefix.prefix}*")
            val ttls = keys.mapNotNull { k ->
                runCatching { redisTemplate.getExpire(k, TimeUnit.SECONDS) }.getOrNull()
            }.filter { it >= 0 }
            RedisPrefixRes(
                prefix = keyPrefix.prefix,
                description = keyPrefix.description,
                keyCount = keys.size.toLong(),
                minTtlSec = ttls.minOrNull(),
                maxTtlSec = ttls.maxOrNull(),
            )
        }
        return RedisCacheRes(prefixes = result)
    }

    fun invalidatePrefix(prefix: String): Long {
        val keys = scanKeys("$prefix*")
        if (keys.isEmpty()) return 0
        return redisTemplate.delete(keys)
    }

    private fun scanKeys(pattern: String): Set<String> {
        val result = mutableSetOf<String>()
        val factory = redisTemplate.connectionFactory ?: return result
        factory.connection.use { conn ->
            conn.keyCommands().scan(ScanOptions.scanOptions().match(pattern).count(500).build()).use { cursor ->
                while (cursor.hasNext()) {
                    result.add(String(cursor.next()))
                }
            }
        }
        return result
    }

    // ─────────── 에러 로그 (DB 기반) ───────────

    fun getErrorLogs(req: ErrorLogsReq): Page<ErrorLogRes> {
        val pageable = PageRequest.of(req.page, req.size, Sort.by(Sort.Direction.DESC, "occurredAt"))
        val spec = Specification<ErrorLog> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            req.acknowledged?.let {
                predicates.add(cb.equal(root.get<Boolean>("acknowledged"), it))
            }
            req.fromDate?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), it.atStartOfDay()))
            }
            req.toDate?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), it.atTime(23, 59, 59, 999_999_999)))
            }
            req.messageKeyword?.takeIf { it.isNotBlank() }?.let {
                val pattern = "%${it.lowercase()}%"
                predicates.add(cb.like(cb.lower(root.get("message")), pattern))
            }
            cb.and(*predicates.toTypedArray())
        }
        val result = errorLogRepository.findAll(spec, pageable)
        val names = resolveNicknames(result.content.mapNotNull { it.acknowledgedBy }.toSet())
        return result.map { ErrorLogRes.from(it, names[it.acknowledgedBy]) }
    }

    @Transactional
    fun acknowledgeErrorLog(logId: Long, req: AcknowledgeLogReq, changedBy: Long): ErrorLogRes {
        val entry = errorLogRepository.findById(logId).orElseThrow {
            IllegalArgumentException("존재하지 않는 에러 로그: $logId")
        }
        val now = LocalDateTime.now()
        if (!entry.acknowledged) {
            // managed entity 라 dirty checking 으로 자동 UPDATE
            entry.acknowledged = true
            entry.acknowledgedBy = changedBy
            entry.acknowledgedAt = now
            entry.acknowledgeNote = req.note
            logAckHistoryRepository.save(LogAckHistory(
                sourceType = AckSourceType.ERROR_LOG,
                sourceId = logId,
                action = AckAction.ACKNOWLEDGE,
                oldNote = null,
                newNote = req.note,
                actedBy = changedBy,
                actedAt = now,
            ))
        } else if (entry.acknowledgeNote != req.note) {
            val oldNote = entry.acknowledgeNote
            entry.acknowledgeNote = req.note
            logAckHistoryRepository.save(LogAckHistory(
                sourceType = AckSourceType.ERROR_LOG,
                sourceId = logId,
                action = AckAction.EDIT_NOTE,
                oldNote = oldNote,
                newNote = req.note,
                actedBy = changedBy,
                actedAt = now,
            ))
        }
        val name = entry.acknowledgedBy?.let { resolveNicknames(setOf(it))[it] }
        return ErrorLogRes.from(entry, name)
    }

    @Transactional
    fun bulkAcknowledgeErrorLogs(req: BulkAcknowledgeReq, changedBy: Long): BulkAcknowledgeRes {
        val appliedNote = req.note?.takeIf { it.isNotBlank() } ?: "일괄 확인"
        val ids = req.ids?.takeIf { it.isNotEmpty() }
        val targets = if (ids != null) {
            errorLogRepository.findByIdInAndAcknowledgedFalse(ids)
        } else {
            errorLogRepository.findByAcknowledgedFalse()
        }
        val now = LocalDateTime.now()
        targets.forEach { entry ->
            entry.acknowledged = true
            entry.acknowledgedBy = changedBy
            entry.acknowledgedAt = now
            entry.acknowledgeNote = appliedNote
            logAckHistoryRepository.save(LogAckHistory(
                sourceType = AckSourceType.ERROR_LOG,
                sourceId = entry.id,
                action = AckAction.BULK_ACKNOWLEDGE,
                oldNote = null,
                newNote = appliedNote,
                actedBy = changedBy,
                actedAt = now,
            ))
        }
        return BulkAcknowledgeRes(processedCount = targets.size, appliedNote = appliedNote)
    }

    fun getUnacknowledgedCount(): UnacknowledgedCountRes {
        val schedulerCount = schedulerLogRepository.countByAcknowledgedFalseAndStatusNot("SUCCESS")
        val errorCount = errorLogRepository.countByAcknowledgedFalse()
        return UnacknowledgedCountRes(schedulerLogs = schedulerCount, errorLogs = errorCount)
    }

    @Transactional
    fun cancelAcknowledgeErrorLog(logId: Long, changedBy: Long): ErrorLogRes {
        val entry = errorLogRepository.findById(logId).orElseThrow {
            IllegalArgumentException("존재하지 않는 에러 로그: $logId")
        }
        if (!entry.acknowledged) {
            return ErrorLogRes.from(entry)
        }
        val oldNote = entry.acknowledgeNote
        // managed entity 라 dirty checking 으로 자동 UPDATE
        entry.acknowledged = false
        entry.acknowledgedBy = null
        entry.acknowledgedAt = null
        entry.acknowledgeNote = null
        logAckHistoryRepository.save(LogAckHistory(
            sourceType = AckSourceType.ERROR_LOG,
            sourceId = logId,
            action = AckAction.CANCEL,
            oldNote = oldNote,
            newNote = null,
            actedBy = changedBy,
        ))
        return ErrorLogRes.from(entry)
    }

    fun getSystemStatus(): SystemStatusRes {
        val runtime = Runtime.getRuntime()
        val maxMem = runtime.maxMemory()
        val usedMem = runtime.totalMemory() - runtime.freeMemory()
        val heapPercent = if (maxMem > 0) ((usedMem * 100) / maxMem).toInt() else 0
        val uptimeSec = ManagementFactory.getRuntimeMXBean().uptime / 1000

        val dbStatus = runCatching {
            entityManager.createNativeQuery("SELECT 1").singleResult
            "UP"
        }.getOrDefault("DOWN")

        val redisStatus = runCatching {
            redisTemplate.connectionFactory?.connection?.ping()
            "UP"
        }.getOrDefault("DOWN")

        val (tomcatActive, tomcatMax) = readTomcatThreadPool()

        return SystemStatusRes(
            dbStatus = dbStatus,
            redisStatus = redisStatus,
            heapUsedMb = usedMem / 1_048_576,
            heapMaxMb = maxMem / 1_048_576,
            heapUsagePercent = heapPercent,
            uptimeSec = uptimeSec,
            jvmThreads = ManagementFactory.getThreadMXBean().threadCount,
            tomcatActive = tomcatActive,
            tomcatMax = tomcatMax,
            fastSchedulerActive = fastScheduler.activeCount,
            fastSchedulerMax = fastScheduler.scheduledThreadPoolExecutor.corePoolSize,
            slowSchedulerActive = slowScheduler.activeCount,
            slowSchedulerMax = slowScheduler.scheduledThreadPoolExecutor.corePoolSize,
        )
    }

    /**
     * Tomcat 워커 스레드 풀 상태를 JMX MBean 에서 조회한다.
     * MBean 이름은 커넥터별(http-nio-8080 등)로 다르므로 패턴 매칭으로 찾는다.
     * 조회 실패 시 (-1, -1) 반환.
     */
    private fun readTomcatThreadPool(): Pair<Int, Int> = runCatching {
        val server = ManagementFactory.getPlatformMBeanServer()
        val names = server.queryNames(ObjectName("Tomcat:type=ThreadPool,name=*"), null)
        val target = names.firstOrNull() ?: return@runCatching -1 to -1
        val busy = (server.getAttribute(target, "currentThreadsBusy") as? Int) ?: -1
        val max = (server.getAttribute(target, "maxThreads") as? Int) ?: -1
        busy to max
    }.getOrElse { -1 to -1 }
}
