package com.example.investfeed.global.ratelimit

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

@Component
class MinIntervalRateLimiter {

    private val nextFreeAt = ConcurrentHashMap<String, AtomicLong>()
    private val lastSweepAt = AtomicLong(nowMs())

    fun reserve(key: String, intervalMs: Long): Long {
        if (intervalMs <= 0) return 0
        val now = nowMs()
        val slot = nextFreeAt.computeIfAbsent(key) { AtomicLong(0) }
        val nextFree = slot.updateAndGet { prev -> max(prev, now) + intervalMs }
        sweepIfDue(now)
        return (nextFree - intervalMs - now).coerceAtLeast(0)
    }

    fun holdFor(key: String, holdMs: Long) {
        if (holdMs <= 0) return
        val until = nowMs() + holdMs
        nextFreeAt.computeIfAbsent(key) { AtomicLong(0) }.updateAndGet { prev -> max(prev, until) }
    }

    private fun nowMs(): Long = System.nanoTime() / 1_000_000

    private fun sweepIfDue(now: Long) {
        val last = lastSweepAt.get()
        if (now - last < SWEEP_INTERVAL_MS) return
        if (!lastSweepAt.compareAndSet(last, now)) return
        nextFreeAt.entries.removeIf { (_, at) -> now - at.get() > STALE_MS }
    }

    companion object {
        private const val SWEEP_INTERVAL_MS = 60 * 1000L
        private const val STALE_MS = 60 * 60 * 1000L
    }
}
