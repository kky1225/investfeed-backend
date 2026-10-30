# investfeed-backend
주식/코인 투자 정보 및 매매 서비스 - Spring Boot + Kotlin 백엔드

## 명령어
- 실행: ./gradlew bootRun --args='--spring.profiles.active=local' (사용자 확인 후)
- 테스트: ./gradlew test
- 작업 종료 시 Stop hook이 ./gradlew testClasses (컴파일 검사)를 자동 실행
- 새 테스트는 Spring 컨텍스트 없이 단위 테스트로 작성
- Kotlin은 파일 단위 lint hook이 없음 (ktlint 미설치)

## 설정 파일
- application.yml: 공통 설정
- application-local.yml: 로컬 환경 시크릿 (DB, API 키) — Read가 차단되어 있으므로 열지 말 것. 접속 정보가 필요하면 사용자에게 요청

## 패키지 구조
- domain/: 클라이언트 직접 통신 API (controller, service, repository, dto)
- internal/: investfeed-assistant(Python)가 호출하는 `/internal/assistant/*` (도구·타임라인)
- common/: 예외, 보안, 유틸 / global/: 설정, 상수, 코루틴, 공휴일, ratelimit
- 그 외 패키지: 외부 API 연동 (kiwoom, toss, upbit, naver, ecos, fred, feargreed, telegram, pressrelease, totp)

## 설계 문서 (docs/)
작업 대상과 관련된 문서만 읽을 것
- recommend-logic.md: 추천 시스템 지표·등급 로직
- trade-logic.md: 모의투자 매매 로직 (지표 정의는 recommend-logic.md 공유)
- us-ext-session-alert.md: 미국 보유종목 장외 급등락 알림 설계
- assistant-requirements.md: AI 비서 요구사항·제약(C-xx) — 확정
- assistant-mvp-scope.md: AI 비서 초기 릴리스 범위 — 확정, 범위 밖 기능은 이 문서를 먼저 수정
- assistant-spec.md: AI 비서 기능 명세
- assistant-architecture.md: AI 비서 구조·인증·연결 방식
- assistant-briefing-content.md: 브리핑·알림 메시지 콘텐츠
- assistant-data-inventory.md: 브리핑 항목별 코드 대조표 (✅/⚠ 항목만 사용)

## DB
- 스키마는 JPA ddl-auto: validate. DDL은 src/main/resources/sql/investfeed.sql 이 기준 (로컬 전용, 저장소에 포함하지 않음)
- 엔티티 변경으로 스키마가 바뀌면 investfeed.sql 에 반영하고, 로컬 DB에 실행할 ALTER 문을 제시할 것
- DDL(ALTER/DROP)과 데이터 삭제(DELETE/TRUNCATE)는 사용자 승인 후에만 실행

## Kotlin 코드 스타일
- 컬렉션 처리는 Kotlin 컬렉션 함수 사용 (map, filter, flatMap 등). Java Stream 사용 금지
- 최신 Kotlin 문법 적용 (data class, sealed class, scope function 등)
- error 발생 시 log.error로 로그 남기기
- 성공 로그는 스케줄러(@Scheduled) 내부에서만 남기기
- domain 패키지의 controller는 요청/응답을 DTO 타입으로 구현
- domain 패키지 외에 외부 API 패키지에서는 상황에 맞게 DTO, parameter 타입 허용
- suspend fun에 @Transactional 금지, Dispatchers.IO/Default 지정 금지(runBlocking 이벤트 루프에서만 실행). 페이싱은 Thread.sleep 대신 global/ratelimit 필터가 담당
- 외부 API 클라이언트 작성 규칙은 .claude/rules/external-api.md 참조

## API 설계 규칙
- 새로 만드는 API는 RESTful 원칙을 준수할 것 (GET: 조회, POST: 생성, PUT: 전체 수정, PATCH: 부분 수정, DELETE: 삭제)
- URL은 리소스 중심의 명사형으로 설계하고, 행위는 HTTP 메서드로 표현할 것
- 기존 API를 REST 형태로 바꾸는 것은 요청이 있을 때만
- 예외: /internal/assistant/tools/* 는 LLM 도구 이름과 맞추기 위해 snake_case 동사형(get_stock_quote 등) 유지

## Git
- git/gh 명령은 hook으로 차단되어 있음. 커밋·브랜치·push는 사용자가 직접 수행
- 작업이 끝나면 변경 파일 목록과 커밋 메시지 초안을 제시할 것

## 필수 규칙
- 소스 파일은 Edit/Write 도구로만 수정할 것. sed·cp·heredoc 등 Bash로 수정하면 lint·Stop hook 검증에서 빠짐
- 추가된 디렉토리의 CLAUDE.md를 반드시 Read로 읽을 것
- 기능 구현 지시가 없으면 질문에 대한 답변이나 제안만 할 것
- 기존 코드 스타일을 유지하되, 개선점은 제안으로 먼저 알릴 것
