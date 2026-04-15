package com.example.investfeed.domain.interest.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.interest.dto.req.AddItemReq
import com.example.investfeed.domain.interest.dto.req.CreateGroupReq
import com.example.investfeed.domain.interest.dto.req.ReorderGroupsReq
import com.example.investfeed.domain.interest.dto.req.ReorderItemsReq
import com.example.investfeed.domain.interest.dto.req.UpdateGroupReq
import com.example.investfeed.domain.interest.dto.res.InterestGroupRes
import com.example.investfeed.domain.interest.dto.res.InterestItemRes
import com.example.investfeed.domain.interest.service.InterestService
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.common.exception.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stock/interest")
class InterestController(
    private val interestService: InterestService
) {

    @GetMapping("groups")
    fun getGroups(
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<ApiResponse<List<InterestGroupRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INTEREST_GROUP_LIST.code,
                message = ResponseCode.INTEREST_GROUP_LIST.message,
                result = interestService.getGroups(user.member.id)
            ), HttpStatus.OK
        )
    }

    @PostMapping("groups")
    fun createGroup(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Valid @RequestBody req: CreateGroupReq
    ): ResponseEntity<ApiResponse<InterestGroupRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INTEREST_GROUP_CREATE.code,
                message = ResponseCode.INTEREST_GROUP_CREATE.message,
                result = interestService.createGroup(user.member.id, req)
            ), HttpStatus.OK
        )
    }

    @PutMapping("groups/{groupId}")
    fun updateGroup(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @Valid @RequestBody req: UpdateGroupReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        interestService.updateGroup(user.member.id, groupId, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INTEREST_GROUP_UPDATE.code,
                message = ResponseCode.INTEREST_GROUP_UPDATE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("groups/{groupId}")
    fun deleteGroup(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        interestService.deleteGroup(user.member.id, groupId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INTEREST_GROUP_DELETE.code,
                message = ResponseCode.INTEREST_GROUP_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PutMapping("groups/reorder")
    fun reorderGroups(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestBody req: ReorderGroupsReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        interestService.reorderGroups(user.member.id, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INTEREST_GROUP_REORDER.code,
                message = ResponseCode.INTEREST_GROUP_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("groups/{groupId}/items")
    fun getItems(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<ApiResponse<List<InterestItemRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INTEREST_ITEM_LIST.code,
                message = ResponseCode.INTEREST_ITEM_LIST.message,
                result = interestService.getItems(user.member.id, groupId)
            ), HttpStatus.OK
        )
    }

    @PostMapping("groups/{groupId}/items")
    fun addItem(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @Valid @RequestBody req: AddItemReq
    ): ResponseEntity<ApiResponse<InterestItemRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INTEREST_ITEM_ADD.code,
                message = ResponseCode.INTEREST_ITEM_ADD.message,
                result = interestService.addItem(user.member.id, groupId, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("groups/{groupId}/items/{itemId}")
    fun removeItem(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @PathVariable itemId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        interestService.removeItem(user.member.id, groupId, itemId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INTEREST_ITEM_DELETE.code,
                message = ResponseCode.INTEREST_ITEM_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PutMapping("groups/{groupId}/items/reorder")
    fun reorderItems(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long,
        @RequestBody req: ReorderItemsReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        interestService.reorderItems(user.member.id, groupId, req)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INTEREST_ITEM_REORDER.code,
                message = ResponseCode.INTEREST_ITEM_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PostMapping("groups/{groupId}/items/stream")
    fun streamItems(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable groupId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        interestService.streamItems(user.member.id, groupId)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INTEREST_ITEM_STREAM.code,
                message = ResponseCode.INTEREST_ITEM_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
