package com.example.investfeed.common.security

object Actions {
    const val READ = "READ"
    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"

    /** 실시간 스트리밍 구독 (POST /stream 등) — POST 지만 read intent. */
    const val SUBSCRIBE = "SUBSCRIBE"
}
