package com.example.investfeed.domain.permission.service

import com.example.investfeed.domain.auth.repository.RoleRepository
import com.example.investfeed.domain.permission.dto.req.UpdateRolePermissionReq
import com.example.investfeed.domain.permission.dto.res.ApiPatternRes
import com.example.investfeed.domain.permission.dto.res.PermissionActionRes
import com.example.investfeed.domain.permission.dto.res.PermissionRes
import com.example.investfeed.domain.permission.dto.res.RolePermissionRes
import com.example.investfeed.domain.permission.entity.Permission
import com.example.investfeed.domain.permission.entity.RolePermission
import com.example.investfeed.domain.permission.repository.PermissionActionRepository
import com.example.investfeed.domain.permission.repository.PermissionRepository
import com.example.investfeed.domain.permission.repository.RolePermissionRepository
import com.example.investfeed.domain.security.CustomUserDetails
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPermissionGrantService(
    private val permissionRepository: PermissionRepository,
    private val permissionActionRepository: PermissionActionRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val roleRepository: RoleRepository,
) {

    fun listPermissions(): List<PermissionRes> {
        return permissionRepository.findAllByOrderByOrderIndexAsc().map { it.toRes() }
    }

    @Transactional
    fun updateRolePermissions(permissionId: Long, req: UpdateRolePermissionReq): PermissionRes {
        val permission = permissionRepository.findById(permissionId)
            .orElseThrow { IllegalArgumentException("권한을 찾을 수 없습니다.") }

        val supportedActions = permissionActionRepository.findAllByPermissionId(permissionId)
            .map { it.action }
            .toSet()

        val currentUserPriority = currentUserRolePriority()

        req.grants.forEach { grant ->
            val role = roleRepository.findByCode(grant.roleCode)
                ?: throw IllegalArgumentException("존재하지 않는 역할입니다: ${grant.roleCode}")

            // hierarchy 검사: 자기보다 낮거나 같은 priority 의 role 권한은 변경 불가
            if (role.priority <= currentUserPriority) {
                throw AccessDeniedException(
                    "본인 (priority=$currentUserPriority) 과 동등 또는 상위 역할 '${role.code}' (priority=${role.priority}) 의 권한은 변경할 수 없습니다."
                )
            }

            val invalid = grant.actions.filterNot { it in supportedActions }
            if (invalid.isNotEmpty()) {
                throw IllegalArgumentException(
                    "권한 ${permission.code} 에 등록되지 않은 action 입니다: ${invalid.joinToString()}. " +
                    "권한 카탈로그에서 먼저 action 을 등록해주세요."
                )
            }

            // JPA flush 순서 보장 위해 명시 flush — INSERT 가 DELETE 보다 먼저 가면 UNIQUE 충돌.
            val existing = rolePermissionRepository.findAllByRoleIdAndPermissionId(role.id, permission.id)
            if (existing.isNotEmpty()) {
                rolePermissionRepository.deleteAll(existing)
                rolePermissionRepository.flush()
            }
            grant.actions.forEach { action ->
                rolePermissionRepository.save(
                    RolePermission(role = role, permission = permission, action = action)
                )
            }
        }

        return permissionRepository.findById(permissionId).get().toRes()
    }

    private fun currentUserRolePriority(): Int {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        val userDetails = auth.principal as? CustomUserDetails
            ?: throw IllegalStateException("인증 정보가 올바르지 않습니다.")
        return userDetails.member.role.priority
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
