package com.example.investfeed.domain.notification.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.notification.dto.req.NotificationListReq
import com.example.investfeed.domain.notification.dto.res.NotificationRes
import com.example.investfeed.domain.notification.service.NotificationService
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.common.exception.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    fun getNotifications(
        @AuthenticationPrincipal user: CustomUserDetails,
        req: NotificationListReq
    ): ResponseEntity<ApiResponse<List<NotificationRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.NOTIFICATION_LIST.code,
                message = ResponseCode.NOTIFICATION_LIST.message,
                result = notificationService.getNotifications(user.member.id, req.assetType)
            ), HttpStatus.OK
        )
    }

    @GetMapping("unread-count")
    fun getUnreadCount(
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<ApiResponse<Int>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.NOTIFICATION_UNREAD_COUNT.code,
                message = ResponseCode.NOTIFICATION_UNREAD_COUNT.message,
                result = notificationService.getUnreadCount(user.member.id)
            ), HttpStatus.OK
        )
    }

    @PutMapping("{id}/read")
    fun markAsRead(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        notificationService.markAsRead(user.member.id, id)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.NOTIFICATION_READ.code,
                message = ResponseCode.NOTIFICATION_READ.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PutMapping("read-all")
    fun markAllAsRead(
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<ApiResponse<Nothing?>> {
        notificationService.markAllAsRead(user.member.id)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.NOTIFICATION_READ_ALL.code,
                message = ResponseCode.NOTIFICATION_READ_ALL.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
