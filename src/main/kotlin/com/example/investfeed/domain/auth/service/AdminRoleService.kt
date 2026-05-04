package com.example.investfeed.domain.auth.service

import com.example.investfeed.domain.auth.dto.req.CreateRoleReq
import com.example.investfeed.domain.auth.dto.req.UpdateRoleOrderReq
import com.example.investfeed.domain.auth.dto.req.UpdateRoleReq
import com.example.investfeed.domain.auth.dto.res.RoleRes
import com.example.investfeed.domain.auth.entity.Role
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.auth.repository.RoleRepository
import com.example.investfeed.domain.security.CustomUserDetails
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminRoleService(
    private val roleRepository: RoleRepository,
    private val memberRepository: MemberRepository,
) {
    fun listRoles(): List<RoleRes> {
        return roleRepository.findAllByOrderByOrderIndexAsc().map { it.toRes() }
    }

    @Transactional
    fun createRole(req: CreateRoleReq): RoleRes {
        if (roleRepository.findByCode(req.code) != null) {
            throw IllegalArgumentException("이미 존재하는 권한 코드입니다: ${req.code}")
        }

        val nextOrderIndex = (roleRepository.findAll().maxOfOrNull { it.orderIndex } ?: -1) + 1
        // 새 role 의 priority: 본인보다 1 낮은 우선순위 (자기보다 하위 role 만 생성 가능)
        val currentPriority = currentUserRolePriority()
        val nextPriority = (roleRepository.findAll().maxOfOrNull { it.priority } ?: currentPriority) + 1

        val saved = roleRepository.save(
            Role(
                code = req.code,
                name = req.name,
                defaultLandingPath = req.defaultLandingPath,
                isSystem = false,
                priority = nextPriority,
                orderIndex = nextOrderIndex,
            )
        )

        return saved.toRes()
    }

    @Transactional
    fun updateRole(id: Long, req: UpdateRoleReq): RoleRes {
        val role = roleRepository.findById(id)
            .orElseThrow { IllegalArgumentException("권한을 찾을 수 없습니다.") }

        assertHierarchyAllowed(role)

        role.name = req.name
        role.defaultLandingPath = req.defaultLandingPath

        return role.toRes()
    }

    @Transactional
    fun deleteRole(id: Long) {
        val role = roleRepository.findById(id)
            .orElseThrow { IllegalArgumentException("권한을 찾을 수 없습니다.") }

        if (role.isSystem) {
            throw IllegalArgumentException("시스템 권한은 삭제할 수 없습니다.")
        }
        assertHierarchyAllowed(role)
        if (memberRepository.existsByRoleId(id)) {
            throw IllegalArgumentException("해당 권한을 사용 중인 회원이 있어 삭제할 수 없습니다.")
        }

        roleRepository.delete(role)
    }

    /** Hierarchy 검사 — 본인과 동등 또는 상위 priority 의 role 은 변경/삭제 불가. */
    private fun assertHierarchyAllowed(target: Role) {
        val currentPriority = currentUserRolePriority()
        if (target.priority <= currentPriority) {
            throw AccessDeniedException(
                "본인 (priority=$currentPriority) 과 동등 또는 상위 역할 '${target.code}' (priority=${target.priority}) 은 변경할 수 없습니다."
            )
        }
    }

    private fun currentUserRolePriority(): Int {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        val userDetails = auth.principal as? CustomUserDetails
            ?: throw IllegalStateException("인증 정보가 올바르지 않습니다.")
        return userDetails.member.role.priority
    }

    @Transactional
    fun updateOrder(req: UpdateRoleOrderReq) {
        val roleMap = roleRepository.findAllById(req.orders.map { it.id }).associateBy { it.id }
        req.orders.forEach { item ->
            val role = roleMap[item.id]
                ?: throw IllegalArgumentException("권한을 찾을 수 없습니다: ${item.id}")
            role.orderIndex = item.orderIndex
        }
    }

    private fun Role.toRes(): RoleRes = RoleRes(
        id = id,
        code = code,
        name = name,
        defaultLandingPath = defaultLandingPath,
        isSystem = isSystem,
        priority = priority,
        orderIndex = orderIndex,
    )
}
