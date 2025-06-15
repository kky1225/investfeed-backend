package com.example.investfeed.kiwoom.dashboard.service

import com.example.investfeed.kiwoom.dashboard.dto.res.DashboardRes
import com.example.investfeed.kiwoom.investor.service.InvestorService
import com.example.investfeed.kiwoom.sect.service.SectService
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class DashboardService(
    private val sectService: SectService,
    private val investorService: InvestorService,
) {
    private val log = KotlinLogging.logger {}

    fun dashboard(

    ): DashboardRes? {
        return null
    }
}