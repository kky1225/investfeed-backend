package com.example.investfeed.domain.recommend.repository

import com.example.investfeed.domain.recommend.entity.StockPick
import org.springframework.data.jpa.repository.JpaRepository

interface StockPickRepository : JpaRepository<StockPick, Long>
