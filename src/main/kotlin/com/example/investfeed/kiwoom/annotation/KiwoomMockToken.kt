package com.example.investfeed.kiwoom.annotation

/**
 * 모의투자 도메인 토큰 보장용. 메서드 실행 전 [com.example.investfeed.kiwoom.auth.service.AuthClient.accessTokenMock]
 * 를 호출해 모의 도메인 토큰(redis 키 분리)을 준비한다. 실거래용 [KiwoomToken] 과 별개 경로.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KiwoomMockToken