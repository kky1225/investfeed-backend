package com.example.investfeed.domain.holding.service

import com.example.investfeed.domain.holding.dto.req.MyBrokerAddReq
import com.example.investfeed.domain.holding.dto.res.*
import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.holding.entity.MemberBroker
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.security.CustomUserDetails
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BrokerService(
    private val brokerRepository: BrokerRepository,
    private val memberBrokerRepository: MemberBrokerRepository,
    private val memberHoldingRepository: MemberHoldingRepository,
) {
    private val log = KotlinLogging.logger {}

    fun listBrokers(): BrokerListRes {
        val brokers = brokerRepository.findAllByOrderByIdAsc()

        return BrokerListRes(
            brokers = brokers.map { broker ->
                BrokerItem(
                    id = broker.id,
                    name = broker.name,
                    type = broker.type,
                    market = broker.market
                )
            }
        )
    }

    fun listMyBrokers(): MyBrokerListRes {
        val memberId = getMemberId()
        val memberBrokers = memberBrokerRepository.findByMemberIdOrderByOrderIndex(memberId)

        return MyBrokerListRes(
            brokers = memberBrokers.map { mb ->
                MyBrokerItem(
                    id = mb.id,
                    brokerId = mb.broker.id,
                    name = mb.broker.name,
                    type = mb.broker.type,
                    market = mb.broker.market,
                    orderIndex = mb.orderIndex
                )
            }
        )
    }

    fun listBrokersByMarket(market: MarketType): BrokerListRes {
        val brokers = brokerRepository.findAllByMarketOrderByIdAsc(market)

        return BrokerListRes(
            brokers = brokers.map { broker ->
                BrokerItem(
                    id = broker.id,
                    name = broker.name,
                    type = broker.type,
                    market = broker.market
                )
            }
        )
    }

    fun listMyBrokersByMarket(market: MarketType): MyBrokerListRes {
        val memberId = getMemberId()
        val memberBrokers = memberBrokerRepository.findByMemberIdAndBrokerMarketOrderByOrderIndex(memberId, market)

        return MyBrokerListRes(
            brokers = memberBrokers.map { mb ->
                MyBrokerItem(
                    id = mb.id,
                    brokerId = mb.broker.id,
                    name = mb.broker.name,
                    type = mb.broker.type,
                    market = mb.broker.market,
                    orderIndex = mb.orderIndex
                )
            }
        )
    }

    @Transactional
    fun addMyBroker(req: MyBrokerAddReq): MyBrokerItem {
        val memberId = getMemberId()
        val broker = brokerRepository.findById(req.brokerId)
            .orElseThrow { IllegalArgumentException("증권사를 찾을 수 없습니다.") }

        val existing = memberBrokerRepository.findByMemberIdAndBrokerId(memberId, broker.id)
        if (existing != null) {
            throw IllegalArgumentException("이미 추가된 증권사입니다.")
        }

        val nextOrder = memberBrokerRepository.countByMemberId(memberId)

        val memberBroker = memberBrokerRepository.save(
            MemberBroker(
                memberId = memberId,
                broker = broker,
                orderIndex = nextOrder
            )
        )

        return MyBrokerItem(
            id = memberBroker.id,
            brokerId = broker.id,
            name = broker.name,
            type = broker.type,
            market = broker.market,
            orderIndex = memberBroker.orderIndex
        )
    }

    @Transactional
    fun removeMyBroker(memberBrokerId: Long) {
        val memberId = getMemberId()
        val memberBroker = memberBrokerRepository.findByMemberIdAndId(memberId, memberBrokerId)
            ?: throw IllegalArgumentException("등록된 증권사를 찾을 수 없습니다.")

        memberHoldingRepository.deleteByMemberIdAndBrokerId(memberId, memberBroker.broker.id)
        memberBrokerRepository.delete(memberBroker)
    }

    private fun getMemberId(): Long {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
    }
}
