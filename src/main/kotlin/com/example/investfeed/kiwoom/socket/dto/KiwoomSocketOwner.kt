package com.example.investfeed.kiwoom.socket.dto

/** 키움 소켓을 쓰는 계정. keyId(앱키 해시)가 같으면 계정이 달라도 연결 하나를 공유한다 (키당 연결 1개 제약) */
data class KiwoomSocketOwner(
    val keyId: String,
    val loginId: String,
    val accessToken: String,
)
