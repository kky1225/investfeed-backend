package com.example.investfeed.domain.recommend.repository

import com.example.investfeed.domain.recommend.entity.StockAvoid
import org.springframework.data.jpa.repository.JpaRepository

interface StockAvoidRepository : JpaRepository<StockAvoid, Long>
