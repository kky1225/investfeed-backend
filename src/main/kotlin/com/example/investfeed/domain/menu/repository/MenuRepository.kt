package com.example.investfeed.domain.menu.repository

import com.example.investfeed.domain.menu.entity.Menu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MenuRepository : JpaRepository<Menu, Long> {

    @Query("SELECT m FROM Menu m LEFT JOIN FETCH m.permissions WHERE m.parent IS NULL ORDER BY m.orderIndex")
    fun findAllRootMenus(): List<Menu>

    fun findByUrl(url: String): Menu?

    fun existsByParentId(parentId: Long): Boolean
}
