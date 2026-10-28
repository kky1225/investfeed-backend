package com.example.investfeed.internal.assistant.dto.req

import com.example.investfeed.internal.assistant.tool.WatchlistToolService

data class MyWatchlistReq(
    val kind: WatchlistToolService.WatchlistKind? = null,
    val group: String? = null,
    val groupId: Long? = null,  // 그룹 목록 카드의 버튼 선택 (LLM 인자 아님, kind 와 함께 온다)
)
