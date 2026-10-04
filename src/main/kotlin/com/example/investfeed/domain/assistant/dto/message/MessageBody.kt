package com.example.investfeed.domain.assistant.dto.message

import java.time.LocalDateTime

data class MessageBody(
    val schemaVersion: Int = 1,
    val type: MessageType,
    val subtype: String? = null,
    val asOf: LocalDateTime? = null,
    val headline: Headline,
    val summary: String = "",
    val sections: List<Section> = emptyList(),
    val cards: List<String> = emptyList(),
    val refs: Map<String, Long> = emptyMap(),
    val turnCards: List<TurnCard> = emptyList(),   // 5단계 Q&A 답변 카드
)

data class TurnCard(
    val kind: String,                 // 도구 이름 또는 카드 종류 (PORTFOLIO / STOCK_CHOICE …)
    val personal: Boolean = false,
    val status: SectionStatus = SectionStatus.OK,
    val ref: String? = null,          // 표시 도구 카드 참조 (확인 카드 버튼용)
    val asOf: LocalDateTime? = null,
    val source: String? = null,
    val error: String? = null,
    val payload: Any? = null,
    val args: Map<String, Any?>? = null,   // 조회 조건 (공개 데이터 카드만). 화면의 조건 기준 안내용
)

data class Headline(
    val text: String,
    val scope: HeadlineScope = HeadlineScope.MARKET,
    val publicText: String? = null,
)

data class Section(
    val id: String,
    val title: String,
    val personal: Boolean = false,
    val status: SectionStatus = SectionStatus.OK,
    val asOf: LocalDateTime? = null,
    val summary: String? = null,
    val account: AccountBlock? = null,
    val text: String = "",
)

data class AccountBlock(
    val total: String,
    val stats: List<AccountStat>,
    val brokers: List<AccountBroker>,
)

data class AccountStat(val label: String, val value: String)

data class AccountBroker(
    val name: String,
    val total: String?,
    val stats: List<AccountStat> = emptyList(),
    val table: String = "",
    val failed: Boolean = false,
)

enum class MessageType { BRIEFING, ALERT, USER, ASSISTANT, SYSTEM }
enum class HeadlineScope { PERSONAL, MARKET }
enum class SectionStatus { OK, EMPTY, FAILED, LOCKED }
