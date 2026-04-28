package com.example.investfeed.domain.menu.service

import com.example.investfeed.domain.auth.entity.Role
import com.example.investfeed.domain.holding.entity.Broker
import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.menu.dto.req.*
import com.example.investfeed.domain.menu.dto.res.MenuPermissionRes
import com.example.investfeed.domain.menu.dto.res.MenuRes
import com.example.investfeed.domain.menu.entity.Menu
import com.example.investfeed.domain.menu.entity.MenuBrokerPermission
import com.example.investfeed.domain.menu.entity.MenuRolePermission
import com.example.investfeed.domain.menu.exception.InvalidBrokerForMenuException
import com.example.investfeed.domain.menu.exception.MenuHasChildrenException
import com.example.investfeed.domain.menu.exception.MenuNotFoundException
import com.example.investfeed.domain.menu.repository.MenuBrokerPermissionRepository
import com.example.investfeed.domain.menu.repository.MenuRepository
import com.example.investfeed.domain.menu.repository.MenuRolePermissionRepository
import mu.KotlinLogging
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MenuService(
    private val menuRepository: MenuRepository,
    private val menuRolePermissionRepository: MenuRolePermissionRepository,
    private val menuBrokerPermissionRepository: MenuBrokerPermissionRepository,
    private val brokerRepository: BrokerRepository
) {
    private val log = KotlinLogging.logger {}

    fun getAllMenuTree(): List<MenuRes> {
        val rootMenus = menuRepository.findAllRootMenus()
        return rootMenus.map { it.toMenuRes() }
    }

    fun getMyMenuTree(role: Role): List<MenuRes> {
        val rootMenus = menuRepository.findAllRootMenus()
        return rootMenus.mapNotNull { it.toAccessibleMenuRes(role) }
    }

    @Transactional
    fun createMenu(req: CreateMenuReq): MenuRes {
        val parent = req.parentId?.let {
            menuRepository.findById(it).orElseThrow { MenuNotFoundException() }
        }

        val brokers = resolveApiBrokers(req.requiredBrokerIds)

        val menu = Menu(
            name = req.name,
            url = req.url,
            icon = req.icon,
            parent = parent,
            orderIndex = req.orderIndex,
            visible = req.visible
        )

        val savedMenu = menuRepository.save(menu)

        Role.entries.forEach { role ->
            menuRolePermissionRepository.save(
                MenuRolePermission(menu = savedMenu, role = role, readable = role == Role.ADMIN)
            )
        }

        brokers.forEach { broker ->
            menuBrokerPermissionRepository.save(
                MenuBrokerPermission(menu = savedMenu, broker = broker)
            )
        }

        return menuRepository.findById(savedMenu.id).orElseThrow { MenuNotFoundException() }.toMenuRes()
    }

    @Transactional
    fun updateMenu(id: Long, req: UpdateMenuReq): MenuRes {
        val menu = menuRepository.findById(id).orElseThrow { MenuNotFoundException() }

        menu.apply {
            name = req.name
            url = req.url
            icon = req.icon
            parent = req.parentId?.let { menuRepository.findById(it).orElseThrow { MenuNotFoundException() } }
            visible = req.visible
        }

        return menu.toMenuRes()
    }

    @Transactional
    fun updateBrokers(menuId: Long, req: UpdateMenuBrokersReq) {
        val menu = menuRepository.findById(menuId).orElseThrow { MenuNotFoundException() }
        val brokers = resolveApiBrokers(req.brokerIds)
        syncBrokerPermissions(menu, brokers)
    }

    @Transactional
    fun deleteMenu(id: Long) {
        if (!menuRepository.existsById(id)) throw MenuNotFoundException()
        if (menuRepository.existsByParentId(id)) throw MenuHasChildrenException()

        menuRepository.deleteById(id)
    }

    @Transactional
    fun updateStructure(req: UpdateMenuStructureReq) {
        val menuMap = menuRepository.findAllById(req.structures.map { it.id }).associateBy { it.id }

        req.structures.forEach { item ->
            val menu = menuMap[item.id] ?: throw MenuNotFoundException()
            menu.parent = item.parentId?.let { parentId ->
                menuMap[parentId] ?: menuRepository.findById(parentId).orElseThrow { MenuNotFoundException() }
            }
            menu.orderIndex = item.orderIndex
        }
    }

    @Transactional
    fun updatePermissions(menuId: Long, req: UpdateMenuPermissionReq) {
        if (!menuRepository.existsById(menuId)) throw MenuNotFoundException()

        req.permissions.forEach { item ->
            val role = Role.valueOf(item.role)
            val permission = menuRolePermissionRepository.findByMenuIdAndRole(menuId, role)

            if (permission != null) {
                permission.readable = item.readable
            } else {
                val menu = menuRepository.findById(menuId).orElseThrow { MenuNotFoundException() }
                menuRolePermissionRepository.save(
                    MenuRolePermission(menu = menu, role = role, readable = item.readable)
                )
            }
        }
    }

    fun checkMenuAccess(url: String, role: Role) {
        val menu = menuRepository.findByUrl(url) ?: return

        if (!isMenuAccessible(menu, role)) {
            throw AccessDeniedException("해당 메뉴에 대한 접근 권한이 없습니다.")
        }
    }

    private fun isMenuAccessible(menu: Menu, role: Role): Boolean {
        menu.parent?.let {
            if (!isMenuAccessible(it, role)) return false
        }

        val permission = menu.permissions.find { it.role == role }
        return permission?.readable ?: false
    }

    private fun resolveApiBrokers(brokerIds: List<Long>): List<Broker> {
        if (brokerIds.isEmpty()) return emptyList()

        val distinctIds = brokerIds.distinct()
        val brokers = brokerRepository.findAllById(distinctIds)

        if (brokers.size != distinctIds.size) {
            val foundIds = brokers.map { it.id }.toSet()
            val missing = distinctIds.filterNot { foundIds.contains(it) }
            throw InvalidBrokerForMenuException("존재하지 않는 broker 입니다: $missing")
        }

        val nonApi = brokers.filter { it.type != BrokerType.API }
        if (nonApi.isNotEmpty()) {
            val names = nonApi.joinToString(", ") { it.name }
            throw InvalidBrokerForMenuException("API 타입 거래소만 권한을 지정할 수 있습니다: $names")
        }

        return brokers
    }

    /**
     * 메뉴의 broker 의존성을 요청 상태와 동기화 (orphanRemoval=true 활용)
     * - 제거 대상: 기존 - 신규
     * - 추가 대상: 신규 - 기존
     */
    private fun syncBrokerPermissions(menu: Menu, brokers: List<Broker>) {
        val targetBrokerIds = brokers.map { it.id }.toSet()
        val currentBrokerIds = menu.brokerPermissions.map { it.broker.id }.toSet()

        menu.brokerPermissions.removeAll { it.broker.id !in targetBrokerIds }

        brokers
            .filter { it.id !in currentBrokerIds }
            .forEach { broker ->
                menu.brokerPermissions.add(MenuBrokerPermission(menu = menu, broker = broker))
            }
    }

    private fun Menu.toMenuRes(): MenuRes = MenuRes(
        id = id,
        name = name,
        url = url,
        icon = icon,
        parentId = parent?.id,
        orderIndex = orderIndex,
        visible = visible,
        permissions = permissions.map { MenuPermissionRes(role = it.role.name, readable = it.readable) },
        requiredBrokerIds = brokerPermissions.map { it.broker.id }.sorted(),
        children = children.sortedBy { it.orderIndex }.map { it.toMenuRes() }
    )

    private fun Menu.toAccessibleMenuRes(role: Role): MenuRes? {
        if (!isMenuAccessible(this, role)) return null
        if (!visible) return null

        val accessibleChildren = children
            .sortedBy { it.orderIndex }
            .mapNotNull { it.toAccessibleMenuRes(role) }

        return MenuRes(
            id = id,
            name = name,
            url = url,
            icon = icon,
            parentId = parent?.id,
            orderIndex = orderIndex,
            visible = visible,
            permissions = permissions
                .filter { it.role == role }
                .map { MenuPermissionRes(role = it.role.name, readable = it.readable) },
            requiredBrokerIds = brokerPermissions.map { it.broker.id }.sorted(),
            children = accessibleChildren
        )
    }
}
