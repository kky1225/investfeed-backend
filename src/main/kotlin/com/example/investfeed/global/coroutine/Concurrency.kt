package com.example.investfeed.global.coroutine

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

suspend fun <T, R> Iterable<T>.mapConcurrently(limit: Int, transform: suspend (T) -> R): List<R> {
    val semaphore = Semaphore(limit)
    return coroutineScope {
        map { item -> async { semaphore.withPermit { transform(item) } } }.awaitAll()
    }
}
