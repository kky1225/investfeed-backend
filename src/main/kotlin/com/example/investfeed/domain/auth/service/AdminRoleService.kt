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
        return roleRepository.findAllByOrderByPriorityAsc().map { it.toRes() }
    }

    @Transactional
    fun createRole(req: CreateRoleReq): RoleRes {
        if (roleRepository.findByCode(req.code) != null) {
            throw IllegalArgumentException("이미 존재하는 권한 코드입니다: ${req.code}")
        }

        val currentPriority = currentUserRolePriority()
        val allRoles = roleRepository.findAll()

        val newPriority = if (req.afterRoleId != null) {
            val afterRole = allRoles.find { it.id == req.afterRoleId }
                ?: throw IllegalArgumentException("기준 역할을 찾을 수 없습니다.")
            if (afterRole.priority < currentPriority) {
                throw AccessDeniedException("본인보다 상위 역할 아래에는 삽입할 수 없습니다.")
            }
            afterRole.priority + 1
        } else {
            currentPriority + 1
        }

        allRoles.filter { it.priority >= newPriority }
            .forEach { it.priority += 1 }

        val nextOrderIndex = (allRoles.maxOfOrNull { it.orderIndex } ?: -1) + 1

        val saved = roleRepository.save(
            Role(
                code = req.code,
                name = req.name,
                defaultLandingPath = req.defaultLandingPath,
                isSystem = false,
                priority = newPriority,
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

        val deletedPriority = role.priority
        roleRepository.delete(role)

        roleRepository.findAll()
            .filter { it.priority > deletedPriority }
            .forEach {
                it.priority -= 1
                it.orderIndex = it.priority
            }
    }

    /** Hierarchy 검사 — 본인과 동등 또는 상위 priority 의 role 은 변경/삭제 불가. */
    private fun assertHierarchyAllowed(target: Role) {
        val currentPriority = currentUserRolePriority()
        if (target.priority <= currentPriority) {
            throw AccessDeniedException(
                "'${target.name}' 역할은 본인과 동등하거나 상위 권한이므로 변경할 수 없습니다."
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
        val currentPriority = currentUserRolePriority()
        val roleMap = roleRepository.findAllById(req.orders.map { it.id }).associateBy { it.id }
        req.orders.forEach { item ->
            val role = roleMap[item.id]
                ?: throw IllegalArgumentException("권한을 찾을 수 없습니다: ${item.id}")
            if (item.priority <= currentPriority) {
                throw AccessDeniedException("'${role.name}' 역할은 본인과 동등하거나 상위 권한이므로 변경할 수 없습니다.")
            }
            role.priority = item.priority
            role.orderIndex = item.priority
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
