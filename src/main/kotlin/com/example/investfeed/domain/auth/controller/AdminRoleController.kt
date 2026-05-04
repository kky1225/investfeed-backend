package com.example.investfeed.domain.auth.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.auth.dto.req.CreateRoleReq
import com.example.investfeed.domain.auth.dto.req.UpdateRoleOrderReq
import com.example.investfeed.domain.auth.dto.req.UpdateRoleReq
import com.example.investfeed.domain.auth.dto.res.RoleRes
import com.example.investfeed.domain.auth.service.AdminRoleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.ADMIN_ROLE)
@RestController
@RequestMapping("/api/admin/roles")
class AdminRoleController(
    private val adminRoleService: AdminRoleService,
) {

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listRoles(): ResponseEntity<ApiResponse<List<RoleRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_ROLE_LIST.code,
                message = ResponseCode.AUTH_ROLE_LIST.message,
                result = adminRoleService.listRoles()
            ), HttpStatus.OK
        )
    }

    @PostMapping
    @RequiresAction(action = Actions.CREATE)
    fun createRole(@Valid @RequestBody req: CreateRoleReq): ResponseEntity<ApiResponse<RoleRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_ROLE_CREATE.code,
                message = ResponseCode.AUTH_ROLE_CREATE.message,
                result = adminRoleService.createRole(req)
            ), HttpStatus.CREATED
        )
    }

    @PutMapping("/{id}")
    @RequiresAction(action = Actions.UPDATE)
    fun updateRole(
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdateRoleReq,
    ): ResponseEntity<ApiResponse<RoleRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_ROLE_UPDATE.code,
                message = ResponseCode.AUTH_ROLE_UPDATE.message,
                result = adminRoleService.updateRole(id, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("/{id}")
    @RequiresAction(action = Actions.DELETE)
    fun deleteRole(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing?>> {
        adminRoleService.deleteRole(id)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_ROLE_DELETE.code,
                message = ResponseCode.AUTH_ROLE_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/order")
    @RequiresAction(action = Actions.UPDATE)
    fun updateOrder(@RequestBody req: UpdateRoleOrderReq): ResponseEntity<ApiResponse<Nothing?>> {
        adminRoleService.updateOrder(req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_ROLE_ORDER_UPDATE.code,
                message = ResponseCode.AUTH_ROLE_ORDER_UPDATE.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
