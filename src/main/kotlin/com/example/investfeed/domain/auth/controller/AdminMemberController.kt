package com.example.investfeed.domain.auth.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.auth.dto.req.ChangeRoleReq
import com.example.investfeed.domain.auth.dto.req.CreateMemberReq
import com.example.investfeed.domain.auth.dto.res.MemberRes
import com.example.investfeed.domain.auth.service.AuthService
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.ADMIN_MEMBER)
@RestController
@RequestMapping("/api/admin/members")
class AdminMemberController(
    private val authService: AuthService
) {
    private val log = KotlinLogging.logger {}

    @PostMapping
    @RequiresAction(action = Actions.CREATE)
    fun createMember(@Valid @RequestBody req: CreateMemberReq): ResponseEntity<ApiResponse<Nothing?>> {
        log.debug { "create member: ${req.loginId}" }

        authService.createMember(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_CREATE_MEMBER.code,
                message = ResponseCode.AUTH_CREATE_MEMBER.message,
                result = null
            ), HttpStatus.CREATED
        )
    }

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun getMembers(): ResponseEntity<ApiResponse<List<MemberRes>>> {
        val members = authService.getMembers()

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_MEMBER_LIST.code,
                message = ResponseCode.AUTH_MEMBER_LIST.message,
                result = members
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/{loginId}/role")
    @RequiresAction(action = Actions.UPDATE)
    fun changeRole(
        @PathVariable loginId: String,
        @RequestBody req: ChangeRoleReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.debug { "change role: $loginId -> ${req.role}" }

        authService.changeRole(loginId, req.role)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_CHANGE_ROLE.code,
                message = ResponseCode.AUTH_CHANGE_ROLE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/{loginId}/totp-reset")
    @RequiresAction(action = Actions.UPDATE)
    fun resetTotp(
        @PathVariable loginId: String
    ): ResponseEntity<ApiResponse<Nothing?>> {

        authService.resetTotp(loginId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_TOTP_RESET.code,
                message = ResponseCode.AUTH_TOTP_RESET.message,
                result = null
            ), HttpStatus.OK
        )
    }

    /**
     * 회원의 2차 비밀번호를 지운다. 회원은 다음에 2차 비밀번호를 새로 만든다.
     * 메서드는 DELETE 지만 회원을 지우는 것이 아니라 회원의 속성을 고치는 일이라, TOTP 초기화·잠금과 같은 UPDATE 권한으로 둔다.
     */
    @DeleteMapping("/{loginId}/secondary-password")
    @RequiresAction(action = Actions.UPDATE)
    fun resetSecondaryPassword(
        @PathVariable loginId: String
    ): ResponseEntity<ApiResponse<Nothing?>> {

        authService.resetSecondaryPassword(loginId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_SECONDARY_RESET.code,
                message = ResponseCode.AUTH_SECONDARY_RESET.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/{loginId}/lock")
    @RequiresAction(action = Actions.UPDATE)
    fun lockAccount(
        @PathVariable loginId: String
    ): ResponseEntity<ApiResponse<Nothing?>> {

        authService.lockAccount(loginId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_LOCK.code,
                message = ResponseCode.AUTH_LOCK.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/{loginId}/api-key-unlock")
    @RequiresAction(action = Actions.UPDATE)
    fun unlockApiKeyRegistration(
        @PathVariable loginId: String
    ): ResponseEntity<ApiResponse<Nothing?>> {

        authService.unlockApiKeyRegistration(loginId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_API_KEY_UNLOCK.code,
                message = ResponseCode.AUTH_API_KEY_UNLOCK.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/{loginId}/unlock")
    @RequiresAction(action = Actions.UPDATE)
    fun unlockAccount(
        @PathVariable loginId: String
    ): ResponseEntity<ApiResponse<Nothing?>> {

        authService.unlockAccount(loginId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_UNLOCK.code,
                message = ResponseCode.AUTH_UNLOCK.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
