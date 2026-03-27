package com.example.investfeed.domain.menu.service

import com.example.investfeed.domain.auth.entity.Role
import com.example.investfeed.domain.menu.dto.req.*
import com.example.investfeed.domain.menu.dto.res.MenuPermissionRes
import com.example.investfeed.domain.menu.dto.res.MenuRes
import com.example.investfeed.domain.menu.entity.Menu
import com.example.investfeed.domain.menu.entity.MenuRolePermission
import com.example.investfeed.domain.menu.exception.MenuHasChildrenException
import com.example.investfeed.domain.menu.exception.MenuNotFoundException
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
    private val menuRolePermissionRepository: MenuRolePermissionRepository
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

    private fun Menu.toMenuRes(): MenuRes = MenuRes(
        id = id,
        name = name,
        url = url,
        icon = icon,
        parentId = parent?.id,
        orderIndex = orderIndex,
        visible = visible,
        permissions = permissions.map { MenuPermissionRes(role = it.role.name, readable = it.readable) },
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
            children = accessibleChildren
        )
    }
}
