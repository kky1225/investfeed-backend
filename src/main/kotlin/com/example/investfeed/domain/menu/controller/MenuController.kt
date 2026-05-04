package com.example.investfeed.domain.menu.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.menu.dto.res.MenuRes
import com.example.investfeed.domain.menu.service.MenuService
import com.example.investfeed.domain.security.CustomUserDetails
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.USER_MENU)
@RestController
@RequestMapping("/api/menus")
class MenuController(
    private val menuService: MenuService
) {

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun getMyMenuTree(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<ApiResponse<List<MenuRes>>> {
        val menus = menuService.getMyMenuTree(userDetails.member.role)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MENU_MY_LIST.code,
                message = ResponseCode.MENU_MY_LIST.message,
                result = menus
            ), HttpStatus.OK
        )
    }
}
