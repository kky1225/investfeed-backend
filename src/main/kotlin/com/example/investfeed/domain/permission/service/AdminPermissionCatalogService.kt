package com.example.investfeed.domain.permission.service

import com.example.investfeed.domain.auth.repository.RoleRepository
import com.example.investfeed.domain.permission.dto.req.AddApiPatternReq
import com.example.investfeed.domain.permission.dto.req.AddPermissionActionReq
import com.example.investfeed.domain.permission.dto.req.CreatePermissionReq
import com.example.investfeed.domain.permission.dto.req.UpdatePermissionReq
import com.example.investfeed.domain.permission.dto.res.ApiPatternRes
import com.example.investfeed.domain.permission.dto.res.PermissionActionRes
import com.example.investfeed.domain.permission.dto.res.PermissionRes
import com.example.investfeed.domain.permission.dto.res.RolePermissionRes
import com.example.investfeed.domain.permission.entity.Permission
import com.example.investfeed.domain.permission.entity.PermissionAction
import com.example.investfeed.domain.permission.entity.PermissionApiPattern
import com.example.investfeed.domain.permission.repository.PermissionActionRepository
import com.example.investfeed.domain.permission.repository.PermissionApiPatternRepository
import com.example.investfeed.domain.permission.repository.PermissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.AntPathMatcher

@Service
@Transactional(readOnly = true)
class AdminPermissionCatalogService(
    private val permissionRepository: PermissionRepository,
    private val permissionApiPatternRepository: PermissionApiPatternRepository,
    private val permissionActionRepository: PermissionActionRepository,
    private val roleRepository: RoleRepository,
) {
    private val antPathMatcher = AntPathMatcher()

    fun listPermissions(): List<PermissionRes> {
        return permissionRepository.findAllByOrderByOrderIndexAsc().map { it.toRes() }
    }

    @Transactional
    fun createPermission(req: CreatePermissionReq): PermissionRes {
        if (permissionRepository.findByCode(req.code) != null) {
            throw IllegalArgumentException("이미 존재하는 권한 코드입니다: ${req.code}")
        }

        req.apiPatterns.forEach { pattern -> assertPatternNotOverlapping(pattern) }

        val nextOrderIndex = (permissionRepository.findAll().maxOfOrNull { it.orderIndex } ?: -1) + 1

        val saved = permissionRepository.save(
            Permission(
                code = req.code,
                name = req.name,
                description = req.description,
                isSystem = false,
                orderIndex = nextOrderIndex,
            )
        )

        req.apiPatterns.forEach { pattern ->
            permissionApiPatternRepository.save(
                PermissionApiPattern(permission = saved, apiPattern = pattern)
            )
        }

        return permissionRepository.findById(saved.id).get().toRes()
    }

    @Transactional
    fun updatePermission(id: Long, req: UpdatePermissionReq): PermissionRes {
        val permission = permissionRepository.findById(id)
            .orElseThrow { IllegalArgumentException("권한을 찾을 수 없습니다.") }
        permission.name = req.name
        permission.description = req.description
        return permission.toRes()
    }

    @Transactional
    fun deletePermission(id: Long) {
        val permission = permissionRepository.findById(id)
            .orElseThrow { IllegalArgumentException("권한을 찾을 수 없습니다.") }
        if (permission.isSystem) {
            throw IllegalArgumentException("시스템 권한은 삭제할 수 없습니다.")
        }
        permissionRepository.delete(permission)
    }

    @Transactional
    fun addApiPattern(permissionId: Long, req: AddApiPatternReq): PermissionRes {
        val permission = permissionRepository.findById(permissionId)
            .orElseThrow { IllegalArgumentException("권한을 찾을 수 없습니다.") }
        assertPatternNotOverlapping(req.apiPattern)
        permissionApiPatternRepository.save(
            PermissionApiPattern(permission = permission, apiPattern = req.apiPattern)
        )
        return permissionRepository.findById(permissionId).get().toRes()
    }

    @Transactional
    fun deleteApiPattern(permissionId: Long, patternId: Long) {
        val pattern = permissionApiPatternRepository.findById(patternId)
            .orElseThrow { IllegalArgumentException("패턴을 찾을 수 없습니다.") }
        if (pattern.permission.id != permissionId) {
            throw IllegalArgumentException("해당 권한의 패턴이 아닙니다.")
        }
        permissionApiPatternRepository.delete(pattern)
    }

    @Transactional
    fun addAction(permissionId: Long, req: AddPermissionActionReq): PermissionRes {
        val permission = permissionRepository.findById(permissionId)
            .orElseThrow { IllegalArgumentException("권한을 찾을 수 없습니다.") }
        if (permissionActionRepository.existsByPermissionIdAndAction(permissionId, req.action)) {
            throw IllegalArgumentException("이미 등록된 action 입니다: ${req.action}")
        }
        permissionActionRepository.save(
            PermissionAction(permission = permission, action = req.action, description = req.description)
        )
        return permissionRepository.findById(permissionId).get().toRes()
    }

    @Transactional
    fun deleteAction(permissionId: Long, action: String) {
        val pa = permissionActionRepository.findByPermissionIdAndAction(permissionId, action)
            ?: throw IllegalArgumentException("등록되지 않은 action 입니다: $action")
        permissionActionRepository.delete(pa)
    }

    private fun assertPatternNotOverlapping(newPattern: String) {
        val existing = permissionApiPatternRepository.findAll()
        val conflicting = existing.firstOrNull { patternsOverlap(newPattern, it.apiPattern) }
        if (conflicting != null) {
            throw IllegalArgumentException("이미 등록된 패턴과 겹칩니다: ${conflicting.apiPattern}")
        }
    }

    private fun patternsOverlap(p1: String, p2: String): Boolean {
        if (p1 == p2) return true
        return antPathMatcher.match(p1, p2) || antPathMatcher.match(p2, p1)
    }

    private fun Permission.toRes(): PermissionRes {
        val allRoles = roleRepository.findAllByOrderByPriorityAsc()
        val grantsByRoleId = rolePermissions.groupBy { it.role.id }
        return PermissionRes(
            id = id,
            code = code,
            name = name,
            description = description,
            isSystem = isSystem,
            orderIndex = orderIndex,
            apiPatterns = apiPatterns.map { ApiPatternRes(it.id, it.apiPattern) },
            supportedActions = actions.map { PermissionActionRes(it.action, it.description) },
            rolePermissions = allRoles.map { role ->
                RolePermissionRes(
                    roleId = role.id,
                    roleCode = role.code,
                    roleName = role.name,
                    actions = grantsByRoleId[role.id]?.map { it.action } ?: emptyList(),
                )
            }
        )
    }
}
