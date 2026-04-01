package com.example.investfeed.domain.holding.service

import com.example.investfeed.domain.holding.dto.req.BrokerCreateReq
import com.example.investfeed.domain.holding.dto.req.BrokerUpdateReq
import com.example.investfeed.domain.holding.dto.res.BrokerItem
import com.example.investfeed.domain.holding.dto.res.BrokerListRes
import com.example.investfeed.domain.holding.entity.Broker
import com.example.investfeed.domain.holding.repository.BrokerRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminBrokerService(
    private val brokerRepository: BrokerRepository,
) {
    private val log = KotlinLogging.logger {}

    fun brokerList(): BrokerListRes {
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

    @Transactional
    fun createBroker(req: BrokerCreateReq): BrokerItem {
        val broker = brokerRepository.save(
            Broker(
                name = req.name,
                type = req.type,
                market = req.market
            )
        )

        return BrokerItem(
            id = broker.id,
            name = broker.name,
            type = broker.type,
            market = broker.market
        )
    }

    @Transactional
    fun updateBroker(brokerId: Long, req: BrokerUpdateReq): BrokerItem {
        val broker = brokerRepository.findById(brokerId)
            .orElseThrow { IllegalArgumentException("증권사를 찾을 수 없습니다.") }

        broker.name = req.name
        broker.type = req.type
        broker.market = req.market

        return BrokerItem(
            id = broker.id,
            name = broker.name,
            type = broker.type,
            market = broker.market
        )
    }

    @Transactional
    fun deleteBroker(brokerId: Long) {
        val broker = brokerRepository.findById(brokerId)
            .orElseThrow { IllegalArgumentException("증권사를 찾을 수 없습니다.") }

        brokerRepository.delete(broker)
    }
}
