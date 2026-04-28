package com.example.investfeed.domain.holding.service

import com.example.investfeed.domain.holding.dto.req.BrokerCreateReq
import com.example.investfeed.domain.holding.dto.req.BrokerUpdateReq
import com.example.investfeed.domain.holding.dto.res.BrokerItem
import com.example.investfeed.domain.holding.dto.res.BrokerListRes
import com.example.investfeed.domain.holding.entity.Broker
import com.example.investfeed.domain.holding.exception.BrokerHasMenuDependencyException
import com.example.investfeed.domain.holding.exception.BrokerNotFoundException
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.menu.repository.MenuBrokerPermissionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminBrokerService(
    private val brokerRepository: BrokerRepository,
    private val menuBrokerPermissionRepository: MenuBrokerPermissionRepository,
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
            .orElseThrow { BrokerNotFoundException() }

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
            .orElseThrow { BrokerNotFoundException() }

        val menuDependencyCount = menuBrokerPermissionRepository.countByBrokerId(brokerId)
        if (menuDependencyCount > 0) {
            throw BrokerHasMenuDependencyException(menuDependencyCount)
        }

        brokerRepository.delete(broker)
    }
}
