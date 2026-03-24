package com.example.investfeed.domain.cryptointerest.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.cryptointerest.dto.req.*
import com.example.investfeed.domain.cryptointerest.dto.res.CryptoInterestGroupRes
import com.example.investfeed.domain.cryptointerest.dto.res.CryptoInterestItemRes
import com.example.investfeed.domain.cryptointerest.service.CryptoInterestService
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.common.exception.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/crypto-interest")
class CryptoInterestController(
    private val cryptoInterestService: CryptoInterestService
) {

    @GetMapping("groups")
    fun getGroups(
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<ApiResponse<List<CryptoInterestGroupRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_INTEREST_GROUP_LIST.code,
                message = ResponseCode.CRYPTO_INTEREST_GROUP_LIST.message,
                result = cryptoInterestService.getGroups(user.member.id)
            ), HttpStatus.OK
        )
    }

    @PostMapping("groups")
    fun createGroup(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestBody req: CreateCryptoGroupReq
    ): ResponseEntity<ApiResponse<CryptoInterestGroupRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_INTEREST_GROUP_CREATE.code,
                message = ResponseCode.CRYPTO_INTEREST_GROUP_CREATE.message,
                result = cryptoInterestService.createGroup(user.member.id, req)
            ), HttpStatus.OK
        )
    }

    @PutMapping("groups/{groupId}")
    fun updateGroup(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @RequestBody req: UpdateCryptoGroupReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoInterestService.updateGroup(user.member.id, groupId, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_INTEREST_GROUP_UPDATE.code,
                message = ResponseCode.CRYPTO_INTEREST_GROUP_UPDATE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("groups/{groupId}")
    fun deleteGroup(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoInterestService.deleteGroup(user.member.id, groupId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_INTEREST_GROUP_DELETE.code,
                message = ResponseCode.CRYPTO_INTEREST_GROUP_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PutMapping("groups/reorder")
    fun reorderGroups(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestBody req: ReorderCryptoGroupsReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoInterestService.reorderGroups(user.member.id, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_INTEREST_GROUP_REORDER.code,
                message = ResponseCode.CRYPTO_INTEREST_GROUP_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("groups/{groupId}/items")
    fun getItems(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<ApiResponse<List<CryptoInterestItemRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_INTEREST_ITEM_LIST.code,
                message = ResponseCode.CRYPTO_INTEREST_ITEM_LIST.message,
                result = cryptoInterestService.getItems(user.member.id, groupId)
            ), HttpStatus.OK
        )
    }

    @PostMapping("groups/{groupId}/items")
    fun addItem(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @RequestBody req: AddCryptoItemReq
    ): ResponseEntity<ApiResponse<CryptoInterestItemRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_INTEREST_ITEM_ADD.code,
                message = ResponseCode.CRYPTO_INTEREST_ITEM_ADD.message,
                result = cryptoInterestService.addItem(user.member.id, groupId, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("groups/{groupId}/items/{itemId}")
    fun removeItem(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @PathVariable itemId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoInterestService.removeItem(user.member.id, groupId, itemId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_INTEREST_ITEM_DELETE.code,
                message = ResponseCode.CRYPTO_INTEREST_ITEM_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PutMapping("groups/{groupId}/items/reorder")
    fun reorderItems(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @RequestBody req: ReorderCryptoItemsReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoInterestService.reorderItems(user.member.id, groupId, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_INTEREST_ITEM_REORDER.code,
                message = ResponseCode.CRYPTO_INTEREST_ITEM_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PostMapping("groups/{groupId}/items/stream")
    fun streamItems(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoInterestService.streamItems(user.member.id, groupId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_INTEREST_ITEM_STREAM.code,
                message = ResponseCode.CRYPTO_INTEREST_ITEM_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
