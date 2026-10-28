package com.example.investfeed.domain.cryptowatchlist.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.cryptowatchlist.dto.req.*
import com.example.investfeed.domain.cryptowatchlist.dto.res.CryptoWatchlistGroupRes
import com.example.investfeed.domain.cryptowatchlist.dto.res.CryptoWatchlistItemRes
import com.example.investfeed.domain.cryptowatchlist.service.CryptoWatchlistService
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

@RequiresAction(permission = Permissions.CRYPTO_WATCHLIST)
@RestController
@RequestMapping("/api/crypto/watchlist")
class CryptoWatchlistController(
    private val cryptoWatchlistService: CryptoWatchlistService
) {

    @GetMapping("groups")
    @RequiresAction(action = Actions.READ)
    fun getGroups(
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<ApiResponse<List<CryptoWatchlistGroupRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_WATCHLIST_GROUP_LIST.code,
                message = ResponseCode.CRYPTO_WATCHLIST_GROUP_LIST.message,
                result = cryptoWatchlistService.getGroups(user.member.id)
            ), HttpStatus.OK
        )
    }

    @PostMapping("groups")
    @RequiresAction(action = Actions.CREATE)
    fun createGroup(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Valid @RequestBody req: CreateCryptoGroupReq
    ): ResponseEntity<ApiResponse<CryptoWatchlistGroupRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_WATCHLIST_GROUP_CREATE.code,
                message = ResponseCode.CRYPTO_WATCHLIST_GROUP_CREATE.message,
                result = cryptoWatchlistService.createGroup(user.member.id, req)
            ), HttpStatus.OK
        )
    }

    @PatchMapping("groups/{groupId}")
    @RequiresAction(action = Actions.UPDATE)
    fun updateGroup(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @Valid @RequestBody req: UpdateCryptoGroupReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoWatchlistService.updateGroup(user.member.id, groupId, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_WATCHLIST_GROUP_UPDATE.code,
                message = ResponseCode.CRYPTO_WATCHLIST_GROUP_UPDATE.message,
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
        cryptoWatchlistService.deleteGroup(user.member.id, groupId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_WATCHLIST_GROUP_DELETE.code,
                message = ResponseCode.CRYPTO_WATCHLIST_GROUP_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("groups/reorder")
    @RequiresAction(action = Actions.UPDATE)
    fun reorderGroups(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestBody req: ReorderCryptoGroupsReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoWatchlistService.reorderGroups(user.member.id, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_WATCHLIST_GROUP_REORDER.code,
                message = ResponseCode.CRYPTO_WATCHLIST_GROUP_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("groups/{groupId}/items")
    @RequiresAction(action = Actions.READ)
    fun getItems(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<ApiResponse<List<CryptoWatchlistItemRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_WATCHLIST_ITEM_LIST.code,
                message = ResponseCode.CRYPTO_WATCHLIST_ITEM_LIST.message,
                result = cryptoWatchlistService.getItems(user.member.id, groupId)
            ), HttpStatus.OK
        )
    }

    @PostMapping("groups/{groupId}/items")
    @RequiresAction(action = Actions.CREATE)
    fun addItem(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @Valid @RequestBody req: AddCryptoItemReq
    ): ResponseEntity<ApiResponse<CryptoWatchlistItemRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_WATCHLIST_ITEM_ADD.code,
                message = ResponseCode.CRYPTO_WATCHLIST_ITEM_ADD.message,
                result = cryptoWatchlistService.addItem(user.member.id, groupId, req)
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
        cryptoWatchlistService.removeItem(user.member.id, groupId, itemId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_WATCHLIST_ITEM_DELETE.code,
                message = ResponseCode.CRYPTO_WATCHLIST_ITEM_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("groups/{groupId}/items/reorder")
    @RequiresAction(action = Actions.UPDATE)
    fun reorderItems(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @RequestBody req: ReorderCryptoItemsReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoWatchlistService.reorderItems(user.member.id, groupId, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_WATCHLIST_ITEM_REORDER.code,
                message = ResponseCode.CRYPTO_WATCHLIST_ITEM_REORDER.message,
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
        cryptoWatchlistService.streamItems(user.member.id, groupId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_WATCHLIST_ITEM_STREAM.code,
                message = ResponseCode.CRYPTO_WATCHLIST_ITEM_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
