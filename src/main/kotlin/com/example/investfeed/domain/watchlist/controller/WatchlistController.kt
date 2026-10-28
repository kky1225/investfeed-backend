package com.example.investfeed.domain.watchlist.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.watchlist.dto.req.AddItemReq
import com.example.investfeed.domain.watchlist.dto.req.CreateGroupReq
import com.example.investfeed.domain.watchlist.dto.req.ReorderGroupsReq
import com.example.investfeed.domain.watchlist.dto.req.ReorderItemsReq
import com.example.investfeed.domain.watchlist.dto.req.UpdateGroupReq
import com.example.investfeed.domain.watchlist.dto.res.WatchlistGroupRes
import com.example.investfeed.domain.watchlist.dto.res.WatchlistItemRes
import com.example.investfeed.domain.watchlist.service.WatchlistService
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.common.exception.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.STOCK_WATCHLIST)
@RestController
@RequestMapping("/api/stock/watchlist")
class WatchlistController(
    private val watchlistService: WatchlistService
) {

    @GetMapping("groups")
    @RequiresAction(action = Actions.READ)
    fun getGroups(
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<ApiResponse<List<WatchlistGroupRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.WATCHLIST_GROUP_LIST.code,
                message = ResponseCode.WATCHLIST_GROUP_LIST.message,
                result = watchlistService.getGroups(user.member.id)
            ), HttpStatus.OK
        )
    }

    @PostMapping("groups")
    @RequiresAction(action = Actions.CREATE)
    fun createGroup(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Valid @RequestBody req: CreateGroupReq
    ): ResponseEntity<ApiResponse<WatchlistGroupRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.WATCHLIST_GROUP_CREATE.code,
                message = ResponseCode.WATCHLIST_GROUP_CREATE.message,
                result = watchlistService.createGroup(user.member.id, req)
            ), HttpStatus.OK
        )
    }

    @PatchMapping("groups/{groupId}")
    @RequiresAction(action = Actions.UPDATE)
    fun updateGroup(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @Valid @RequestBody req: UpdateGroupReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        watchlistService.updateGroup(user.member.id, groupId, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.WATCHLIST_GROUP_UPDATE.code,
                message = ResponseCode.WATCHLIST_GROUP_UPDATE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("groups/{groupId}")
    @RequiresAction(action = Actions.DELETE)
    fun deleteGroup(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        watchlistService.deleteGroup(user.member.id, groupId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.WATCHLIST_GROUP_DELETE.code,
                message = ResponseCode.WATCHLIST_GROUP_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("groups/reorder")
    @RequiresAction(action = Actions.UPDATE)
    fun reorderGroups(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestBody req: ReorderGroupsReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        watchlistService.reorderGroups(user.member.id, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.WATCHLIST_GROUP_REORDER.code,
                message = ResponseCode.WATCHLIST_GROUP_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("groups/{groupId}/items")
    @RequiresAction(action = Actions.READ)
    fun getItems(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<ApiResponse<List<WatchlistItemRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.WATCHLIST_ITEM_LIST.code,
                message = ResponseCode.WATCHLIST_ITEM_LIST.message,
                result = watchlistService.getItems(user.member.id, groupId)
            ), HttpStatus.OK
        )
    }

    @PostMapping("groups/{groupId}/items")
    @RequiresAction(action = Actions.CREATE)
    fun addItem(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @Valid @RequestBody req: AddItemReq
    ): ResponseEntity<ApiResponse<WatchlistItemRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.WATCHLIST_ITEM_ADD.code,
                message = ResponseCode.WATCHLIST_ITEM_ADD.message,
                result = watchlistService.addItem(user.member.id, groupId, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("groups/{groupId}/items/{itemId}")
    @RequiresAction(action = Actions.DELETE)
    fun removeItem(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @PathVariable itemId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        watchlistService.removeItem(user.member.id, groupId, itemId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.WATCHLIST_ITEM_DELETE.code,
                message = ResponseCode.WATCHLIST_ITEM_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("groups/{groupId}/items/reorder")
    @RequiresAction(action = Actions.UPDATE)
    fun reorderItems(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @RequestBody req: ReorderItemsReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        watchlistService.reorderItems(user.member.id, groupId, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.WATCHLIST_ITEM_REORDER.code,
                message = ResponseCode.WATCHLIST_ITEM_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("groups/{groupId}/items/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamItems(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        watchlistService.streamItems(user.member.id, groupId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.WATCHLIST_ITEM_STREAM.code,
                message = ResponseCode.WATCHLIST_ITEM_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
