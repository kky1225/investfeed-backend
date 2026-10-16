package com.example.investfeed.kiwoom.socket

import com.fasterxml.jackson.databind.JsonNode

/**
 * 상시 구독 항목(전용 그룹 9)의 실시간 수신을 받는 쪽. 키움 연동 패키지가 도메인 구체 클래스를 모르도록 이 인터페이스로만 연결한다.
 * 구현체는 [KiwoomSocketManager.addHandler] 로 직접 등록한다 (현재: 서킷브레이커 판정 CircuitBreakerService).
 * 소켓 수신 스레드에서 동기로 불리므로 메모리 기록처럼 가벼운 일만 할 것
 */
interface PersistentStreamHandler {
    /** type: 실시간 항목(0J 등), item: 종목·업종 코드, values: 실시간 값(필드번호 → 값) */
    fun onPersistentTick(type: String, item: String, values: JsonNode?)
}
