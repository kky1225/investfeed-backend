package com.example.investfeed.domain.realizedpnl.repository

import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.realizedpnl.entity.MemberRealizedPnl
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRealizedPnlRepository : JpaRepository<MemberRealizedPnl, Long> {

    fun findByMemberIdAndId(memberId: Long, id: Long): MemberRealizedPnl?

    fun findByMemberIdAndBrokerIdAndYearAndMonth(memberId: Long, brokerId: Long, year: Int, month: Int): MemberRealizedPnl?

    fun findByMemberIdAndBrokerMarketAndYearAndMonthOrderByYearDescMonthDesc(memberId: Long, market: MarketType, year: Int, month: Int): List<MemberRealizedPnl>

    fun findByMemberIdAndBrokerMarketAndYearOrderByYearDescMonthDesc(memberId: Long, market: MarketType, year: Int): List<MemberRealizedPnl>

    fun findByMemberIdAndBrokerMarketOrderByYearDescMonthDesc(memberId: Long, market: MarketType): List<MemberRealizedPnl>

    fun findByMemberIdAndYear(memberId: Long, year: Int): List<MemberRealizedPnl>

    fun findByMemberId(memberId: Long): List<MemberRealizedPnl>
}
