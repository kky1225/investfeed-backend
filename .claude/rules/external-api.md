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

## Upbit Open API
- 공식 문서: https://docs.upbit.com/kr/reference/api-overview
- 명세가 필요하면 위 페이지에서 직접 조회 가능

## 키움 Open API
- 공식 문서: https://openapi.kiwoom.com/guide/apiguide?dummyVal=0
- 명세 PDF: ~/Downloads/키움 REST API 문서.pdf (로컬에만 있음. pdftotext로 필요한 페이지만 추출해서 볼 것)

## 기타 외부 API
- 명세가 없는 경우 웹 검색 또는 해당 페이지에서 직접 조회
