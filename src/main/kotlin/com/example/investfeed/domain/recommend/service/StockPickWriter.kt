package com.example.investfeed.domain.recommend.service

import com.example.investfeed.domain.recommend.entity.StockPick
import com.example.investfeed.domain.recommend.entity.StockPickHistory
import com.example.investfeed.domain.recommend.repository.StockPickHistoryRepository
import com.example.investfeed.domain.recommend.repository.StockPickRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/** 추천 배치의 stock_pick 쓰기. 외부 API 호출이 끝난 뒤 짧은 트랜잭션으로 실행한다 */
@Service
class StockPickWriter(
    private val stockPickRepository: StockPickRepository,
    private val stockPickHistoryRepository: StockPickHistoryRepository,
) {

    @Transactional
    fun replace(picks: List<StockPick>, history: List<StockPickHistory>, pickDay: LocalDate) {
        // 현재용 테이블 갱신
        stockPickRepository.deleteAll()
        stockPickRepository.saveAll(picks)

        // 이력용 테이블 — 같은 날 재실행 시 "오늘분"을 최신 실행분으로 교체(stk_cd·일자당 1건).
        stockPickHistoryRepository.deleteByPickDateBetween(
            pickDay.atStartOfDay(),
            pickDay.atTime(23, 59, 59),
        )
        stockPickHistoryRepository.saveAll(history)
    }

    /** 배치 실패 시 지난 추천이 다음 날 모의투자에 쓰이지 않도록 비운다. 이력은 건드리지 않는다 */
    @Transactional
    fun clearCurrent() {
        stockPickRepository.deleteAll()
    }
}
