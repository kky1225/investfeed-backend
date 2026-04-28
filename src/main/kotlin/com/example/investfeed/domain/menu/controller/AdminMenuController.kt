package com.example.investfeed.domain.menu.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.menu.dto.req.CreateMenuReq
import com.example.investfeed.domain.menu.dto.req.UpdateMenuBrokersReq
import com.example.investfeed.domain.menu.dto.req.UpdateMenuPermissionReq
import com.example.investfeed.domain.menu.dto.req.UpdateMenuReq
import com.example.investfeed.domain.menu.dto.req.UpdateMenuStructureReq
import com.example.investfeed.domain.menu.dto.res.MenuRes
import com.example.investfeed.domain.menu.service.MenuService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/menus")
@PreAuthorize("hasRole('ADMIN')")
class AdminMenuController(
    private val menuService: MenuService
) {

    @GetMapping
    fun getAllMenuTree(): ResponseEntity<ApiResponse<List<MenuRes>>> {
        val menus = menuService.getAllMenuTree()

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MENU_LIST.code,
                message = ResponseCode.MENU_LIST.message,
                result = menus
            ), HttpStatus.OK
        )
    }

    @PostMapping
    fun createMenu(@Valid @RequestBody req: CreateMenuReq): ResponseEntity<ApiResponse<MenuRes>> {
        val menu = menuService.createMenu(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MENU_CREATE.code,
                message = ResponseCode.MENU_CREATE.message,
                result = menu
            ), HttpStatus.CREATED
        )
    }

    @PutMapping("/{id}")
    fun updateMenu(
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdateMenuReq
    ): ResponseEntity<ApiResponse<MenuRes>> {
        val menu = menuService.updateMenu(id, req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MENU_UPDATE.code,
                message = ResponseCode.MENU_UPDATE.message,
                result = menu
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("/{id}")
    fun deleteMenu(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing?>> {
        menuService.deleteMenu(id)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MENU_DELETE.code,
                message = ResponseCode.MENU_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/structure")
    fun updateStructure(@RequestBody req: UpdateMenuStructureReq): ResponseEntity<ApiResponse<Nothing?>> {
        menuService.updateStructure(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MENU_STRUCTURE_UPDATE.code,
                message = ResponseCode.MENU_STRUCTURE_UPDATE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/{id}/permissions")
    fun updatePermissions(
        @PathVariable id: Long,
        @RequestBody req: UpdateMenuPermissionReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        menuService.updatePermissions(id, req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MENU_PERMISSION_UPDATE.code,
                message = ResponseCode.MENU_PERMISSION_UPDATE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/{id}/brokers")
    fun updateBrokers(
        @PathVariable id: Long,
        @RequestBody req: UpdateMenuBrokersReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        menuService.updateBrokers(id, req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MENU_BROKER_UPDATE.code,
                message = ResponseCode.MENU_BROKER_UPDATE.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
