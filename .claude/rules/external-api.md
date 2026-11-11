---
paths:
  - "src/main/kotlin/com/example/investfeed/kiwoom/**"
  - "src/main/kotlin/com/example/investfeed/toss/**"
  - "src/main/kotlin/com/example/investfeed/upbit/**"
  - "src/main/kotlin/com/example/investfeed/feargreed/**"
  - "src/main/kotlin/com/example/investfeed/global/**"
  - "src/main/kotlin/com/example/investfeed/totp/**"
  - "src/main/kotlin/com/example/investfeed/naver/**"
  - "src/main/kotlin/com/example/investfeed/ecos/**"
  - "src/main/kotlin/com/example/investfeed/fred/**"
  - "src/main/kotlin/com/example/investfeed/telegram/**"
  - "src/main/kotlin/com/example/investfeed/pressrelease/**"
---

# 외부 API 연동 규칙

## 클라이언트 클래스 스타일
- 한 스타일로 통일: 메서드 하나라도 async/suspend 문맥에서 호출되면 클래스 전체 suspend fun(+awaitBodyOrNull), 아니면 클래스 전체 동기(.block())
- 단일 동기 호출에 runBlocking 래퍼를 만들지 말 것

## WebClient·오류 처리 (모든 외부 API 공통)
- WebClient 는 설정 클래스에서 ExternalWebClientFactory.create 로만 만든다 (집계·간격 제한·ApiCallMetricsFilter 고정, API 별 필터는 innerFilters). 클라이언트 안에서 WebClient.builder() 금지
- 재시도·호출 간격은 필터(global/ratelimit)가 맡는다. 클라이언트에서 for 문 재시도·Thread.sleep 금지
- HTTP 오류는 onStatus + res.logHttpError(provider) 한 가지. 주소에 키·토큰이 있으면 secrets 로 가린다
- 마지막 catch 는 log.apiFailure(e) + ExternalApiFailureException. 주소에 키가 있으면 apiFailure(e, secretInUrl = true) + cause 없이 던짐
- 경로에 키·토큰이 있는 API 는 factory 에 secrets = listOf(키 값) (호출 상세 debug 로그에서 경로는 남기고 그 값 자리만 {secret} 으로 가림). 클라이언트와 같은 @Value 속성을 설정 클래스에서도 받는다

## Upbit Open API
- 공식 문서: https://docs.upbit.com/kr/reference/api-overview
- 명세가 필요하면 위 페이지에서 직접 조회 가능

## 키움 Open API
- 공식 문서: https://openapi.kiwoom.com/guide/apiguide?dummyVal=0
- 명세 PDF: ~/Downloads/키움 REST API 문서.pdf (로컬에만 있음. pdftotext로 필요한 페이지만 추출해서 볼 것)

## 기타 외부 API
- 명세가 없는 경우 웹 검색 또는 해당 페이지에서 직접 조회
