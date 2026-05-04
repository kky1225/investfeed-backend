package com.example.investfeed.domain.permission.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.permission.dto.req.UpdateRolePermissionReq
import com.example.investfeed.domain.permission.dto.res.PermissionRes
import com.example.investfeed.domain.permission.service.AdminPermissionGrantService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.ADMIN_PERMISSION_GRANT)
@RestController
@RequestMapping("/api/admin/permissions/grants")
class AdminPermissionGrantController(
    private val service: AdminPermissionGrantService,
) {

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listPermissions(): ResponseEntity<ApiResponse<List<PermissionRes>>> {
        return ResponseEntity(
            ApiResponse(ResponseCode.PERMISSION_LIST.code, ResponseCode.PERMISSION_LIST.message,
                service.listPermissions()), HttpStatus.OK
        )
    }

    @PatchMapping("/{id}")
    @RequiresAction(action = Actions.UPDATE)
    fun updateRolePermissions(
        @PathVariable id: Long,
        @RequestBody req: UpdateRolePermissionReq,
    ): ResponseEntity<ApiResponse<PermissionRes>> {
        return ResponseEntity(
            ApiResponse(ResponseCode.PERMISSION_ROLE_UPDATE.code, ResponseCode.PERMISSION_ROLE_UPDATE.message,
                service.updateRolePermissions(id, req)), HttpStatus.OK
        )
    }
}
