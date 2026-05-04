package com.example.investfeed.domain.menu.service

import com.example.investfeed.common.security.Actions
import com.example.investfeed.domain.auth.entity.Role
import com.example.investfeed.domain.holding.entity.Broker
import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.menu.dto.req.*
import com.example.investfeed.domain.menu.dto.res.MenuRes
import com.example.investfeed.domain.menu.entity.Menu
import com.example.investfeed.domain.menu.entity.MenuBrokerPermission
import com.example.investfeed.domain.menu.exception.InvalidBrokerForMenuException
import com.example.investfeed.domain.menu.exception.MenuHasChildrenException
import com.example.investfeed.domain.menu.exception.MenuNotFoundException
import com.example.investfeed.domain.menu.repository.MenuBrokerPermissionRepository
import com.example.investfeed.domain.menu.repository.MenuRepository
import com.example.investfeed.domain.permission.repository.PermissionRepository
import com.example.investfeed.domain.permission.repository.RolePermissionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MenuService(
    private val menuRepository: MenuRepository,
    private val menuBrokerPermissionRepository: MenuBrokerPermissionRepository,
    private val brokerRepository: BrokerRepository,
    private val permissionRepository: PermissionRepository,
    private val rolePermissionRepository: RolePermissionRepository,
) {
    private val log = KotlinLogging.logger {}

    fun getAllMenuTree(): List<MenuRes> {
        val rootMenus = menuRepository.findAllRootMenus()
        return rootMenus.map { it.toMenuRes() }
    }

    fun getMyMenuTree(role: Role): List<MenuRes> {
        val grantedPermissionIds = rolePermissionRepository.findAllByRoleId(role.id)
            .filter { it.action == Actions.READ }
            .map { it.permission.id }
            .toSet()

        val rootMenus = menuRepository.findAllRootMenus()
        return rootMenus.mapNotNull { it.toAccessibleMenuRes(grantedPermissionIds) }
    }

    @Transactional
    fun createMenu(req: CreateMenuReq): MenuRes {
        val parent = req.parentId?.let {
            menuRepository.findById(it).orElseThrow { MenuNotFoundException() }
        }

        val requiredPermission = req.requiredPermissionId?.let {
            permissionRepository.findById(it).orElseThrow { IllegalArgumentException("권한을 찾을 수 없습니다: $it") }
        }

        val brokers = resolveApiBrokers(req.requiredBrokerIds)

        val menu = Menu(
            name = req.name,
            url = req.url,
            icon = req.icon,
            parent = parent,
            requiredPermission = requiredPermission,
            orderIndex = req.orderIndex,
            visible = req.visible
        )

        val savedMenu = menuRepository.save(menu)

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
            requiredPermission = req.requiredPermissionId?.let {
                permissionRepository.findById(it).orElseThrow { IllegalArgumentException("권한을 찾을 수 없습니다: $it") }
            }
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
        requiredPermissionId = requiredPermission?.id,
        requiredPermissionCode = requiredPermission?.code,
        requiredPermissionName = requiredPermission?.name,
        orderIndex = orderIndex,
        visible = visible,
        requiredBrokerIds = brokerPermissions.map { it.broker.id }.sorted(),
        children = children.sortedBy { it.orderIndex }.map { it.toMenuRes() }
    )

    /**
     * 사이드바 트리에 노출할 메뉴인지 판정 + 자식 재귀.
     *
     * selfAllowed 규칙:
     * - requiredPermission 있음 → 사용자가 해당 권한 canRead=true 일 때만 본인 노출
     * - requiredPermission 없음 + 자식 있음 (네비 그룹) → 본인은 단독으로 노출 X. 자식이 보일 때만 따라 노출
     * - requiredPermission 없음 + 자식 없음 (단독 메뉴) → 항상 노출
     *
     * 최종 노출: selfAllowed 또는 자식 중 하나라도 노출 가능한 경우.
     */
    private fun Menu.toAccessibleMenuRes(grantedPermissionIds: Set<Long>): MenuRes? {
        if (!visible) return null

        val accessibleChildren = children
            .sortedBy { it.orderIndex }
            .mapNotNull { it.toAccessibleMenuRes(grantedPermissionIds) }

        val selfAllowed = if (requiredPermission != null) {
            requiredPermission!!.id in grantedPermissionIds
        } else {
            !children.isNotEmpty()
        }

        if (!selfAllowed && accessibleChildren.isEmpty()) return null

        return MenuRes(
            id = id,
            name = name,
            url = url,
            icon = icon,
            parentId = parent?.id,
            requiredPermissionId = requiredPermission?.id,
            requiredPermissionCode = requiredPermission?.code,
            requiredPermissionName = requiredPermission?.name,
            orderIndex = orderIndex,
            visible = visible,
            requiredBrokerIds = brokerPermissions.map { it.broker.id }.sorted(),
            children = accessibleChildren
        )
    }
}
