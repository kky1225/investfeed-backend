package com.example.investfeed.domain.papertrade.repository

import com.example.investfeed.domain.papertrade.entity.PaperFill
import org.springframework.data.jpa.repository.JpaRepository

interface PaperFillRepository : JpaRepository<PaperFill, Long>