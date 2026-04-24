package com.example.investfeed.domain.notification.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.notification.dto.req.NotificationListReq
import com.example.investfeed.domain.notification.dto.req.NotificationSettingReq
import com.example.investfeed.domain.notification.dto.req.PriceTargetCreateReq
import com.example.investfeed.domain.notification.dto.res.NotificationRes
import com.example.investfeed.domain.notification.dto.res.NotificationSettingRes
import com.example.investfeed.domain.notification.dto.res.PriceTargetRes
import com.example.investfeed.domain.notification.service.NotificationService
import com.example.investfeed.domain.notification.service.NotificationSettingService
import com.example.investfeed.domain.notification.service.PriceTargetService
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.common.exception.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val priceTargetService: PriceTargetService,
    private val notificationSettingService: NotificationSettingService,
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

    @PatchMapping("{id}/read")
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

    @PatchMapping("read-all")
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

    @PostMapping("price-targets")
    fun createPriceTarget(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Valid @RequestBody req: PriceTargetCreateReq
    ): ResponseEntity<ApiResponse<PriceTargetRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.PRICE_TARGET_CREATE.code,
                message = ResponseCode.PRICE_TARGET_CREATE.message,
                result = priceTargetService.createPriceTarget(user.member.id, req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("price-targets")
    fun getPriceTargets(
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<ApiResponse<List<PriceTargetRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.PRICE_TARGET_LIST.code,
                message = ResponseCode.PRICE_TARGET_LIST.message,
                result = priceTargetService.getPriceTargets(user.member.id)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("price-targets/{id}")
    fun deletePriceTarget(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        priceTargetService.deletePriceTarget(user.member.id, id)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.PRICE_TARGET_DELETE.code,
                message = ResponseCode.PRICE_TARGET_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("settings")
    fun getNotificationSetting(): ResponseEntity<ApiResponse<NotificationSettingRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.NOTIFICATION_SETTING.code,
                message = ResponseCode.NOTIFICATION_SETTING.message,
                result = notificationSettingService.getSetting()
            ), HttpStatus.OK
        )
    }

    @PutMapping("settings")
    fun saveNotificationSetting(
        @RequestBody req: NotificationSettingReq
    ): ResponseEntity<ApiResponse<NotificationSettingRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.NOTIFICATION_SETTING_SAVE.code,
                message = ResponseCode.NOTIFICATION_SETTING_SAVE.message,
                result = notificationSettingService.saveSetting(req)
            ), HttpStatus.OK
        )
    }
}
