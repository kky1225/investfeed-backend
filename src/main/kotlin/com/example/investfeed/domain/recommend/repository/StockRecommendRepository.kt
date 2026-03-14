package com.example.investfeed.domain.recommend.repository

import com.example.investfeed.domain.recommend.entity.StockRecommend
import org.springframework.data.jpa.repository.JpaRepository

interface StockRecommendRepository : JpaRepository<StockRecommend, Long>
