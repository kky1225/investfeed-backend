package com.example.investfeed.domain.permission.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.permission.dto.req.AddApiPatternReq
import com.example.investfeed.domain.permission.dto.req.AddPermissionActionReq
import com.example.investfeed.domain.permission.dto.req.CreatePermissionReq
import com.example.investfeed.domain.permission.dto.req.UpdatePermissionReq
import com.example.investfeed.domain.permission.dto.res.PermissionRes
import com.example.investfeed.domain.permission.service.AdminPermissionCatalogService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.ADMIN_PERMISSION_CATALOG)
@RestController
@RequestMapping("/api/admin/permissions/catalog")
class AdminPermissionCatalogController(
    private val service: AdminPermissionCatalogService,
) {

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listPermissions(): ResponseEntity<ApiResponse<List<PermissionRes>>> {
        return ResponseEntity(
            ApiResponse(ResponseCode.PERMISSION_LIST.code, ResponseCode.PERMISSION_LIST.message,
                service.listPermissions()), HttpStatus.OK
        )
    }

    @PostMapping
    @RequiresAction(action = Actions.CREATE)
    fun createPermission(@Valid @RequestBody req: CreatePermissionReq): ResponseEntity<ApiResponse<PermissionRes>> {
        return ResponseEntity(
            ApiResponse(ResponseCode.PERMISSION_CREATE.code, ResponseCode.PERMISSION_CREATE.message,
                service.createPermission(req)), HttpStatus.CREATED
        )
    }

    @PutMapping("/{id}")
    @RequiresAction(action = Actions.UPDATE)
    fun updatePermission(
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdatePermissionReq,
    ): ResponseEntity<ApiResponse<PermissionRes>> {
        return ResponseEntity(
            ApiResponse(ResponseCode.PERMISSION_UPDATE.code, ResponseCode.PERMISSION_UPDATE.message,
                service.updatePermission(id, req)), HttpStatus.OK
        )
    }

    @DeleteMapping("/{id}")
    @RequiresAction(action = Actions.DELETE)
    fun deletePermission(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing?>> {
        service.deletePermission(id)
        return ResponseEntity(
            ApiResponse(ResponseCode.PERMISSION_DELETE.code, ResponseCode.PERMISSION_DELETE.message, null),
            HttpStatus.OK
        )
    }

    @PostMapping("/{id}/patterns")
    @RequiresAction(action = Actions.CREATE)
    fun addApiPattern(
        @PathVariable id: Long,
        @Valid @RequestBody req: AddApiPatternReq,
    ): ResponseEntity<ApiResponse<PermissionRes>> {
        return ResponseEntity(
            ApiResponse(ResponseCode.PERMISSION_PATTERN_ADD.code, ResponseCode.PERMISSION_PATTERN_ADD.message,
                service.addApiPattern(id, req)), HttpStatus.OK
        )
    }

    @DeleteMapping("/{id}/patterns/{patternId}")
    @RequiresAction(action = Actions.DELETE)
    fun deleteApiPattern(
        @PathVariable id: Long,
        @PathVariable patternId: Long,
    ): ResponseEntity<ApiResponse<Nothing?>> {
        service.deleteApiPattern(id, patternId)
        return ResponseEntity(
            ApiResponse(ResponseCode.PERMISSION_PATTERN_DELETE.code, ResponseCode.PERMISSION_PATTERN_DELETE.message,
                null), HttpStatus.OK
        )
    }

    @PostMapping("/{id}/actions")
    @RequiresAction(action = Actions.CREATE)
    fun addAction(
        @PathVariable id: Long,
        @Valid @RequestBody req: AddPermissionActionReq,
    ): ResponseEntity<ApiResponse<PermissionRes>> {
        return ResponseEntity(
            ApiResponse(ResponseCode.PERMISSION_ACTION_ADD.code, ResponseCode.PERMISSION_ACTION_ADD.message,
                service.addAction(id, req)), HttpStatus.OK
        )
    }

    @DeleteMapping("/{id}/actions/{action}")
    @RequiresAction(action = Actions.DELETE)
    fun deleteAction(
        @PathVariable id: Long,
        @PathVariable action: String,
    ): ResponseEntity<ApiResponse<Nothing?>> {
        service.deleteAction(id, action)
        return ResponseEntity(
            ApiResponse(ResponseCode.PERMISSION_ACTION_DELETE.code, ResponseCode.PERMISSION_ACTION_DELETE.message,
                null), HttpStatus.OK
        )
    }
}
