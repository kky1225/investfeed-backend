package com.example.investfeed.domain.auth.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.auth.dto.req.ApiKeyReq
import com.example.investfeed.domain.auth.dto.req.ChangePasswordReq
import com.example.investfeed.domain.auth.dto.req.ChangeRoleReq
import com.example.investfeed.domain.auth.dto.req.CreateMemberReq
import com.example.investfeed.domain.auth.dto.req.LoginReq
import com.example.investfeed.domain.auth.dto.req.UpdateProfileReq
import com.example.investfeed.domain.auth.dto.res.ApiKeyRes
import com.example.investfeed.domain.auth.dto.res.MemberRes
import com.example.investfeed.domain.auth.dto.res.TokenRes
import com.example.investfeed.domain.auth.service.AuthService
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.common.exception.ApiResponse
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    @param:Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,
    @param:Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long,
    @param:Value("\${cookie.secure}")
    private val cookieSecure: Boolean,
    private val authService: AuthService
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("/login")
    fun login(
        @RequestBody req: LoginReq,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<TokenRes>> {
        log.info { "login: ${req.loginId}" }

        val result = authService.login(req)
        response.addHeader(HttpHeaders.SET_COOKIE, createAccessTokenCookie(result.accessToken))
        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(result.refreshToken))

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_LOGIN.code,
                message = ResponseCode.AUTH_LOGIN.message,
                result = result.tokenRes
            ), HttpStatus.OK
        )
    }

    @PostMapping("/reissue")
    fun reissue(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<TokenRes>> {
        val result = authService.reissue(refreshToken)
        response.addHeader(HttpHeaders.SET_COOKIE, createAccessTokenCookie(result.accessToken))

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_REISSUE.code,
                message = ResponseCode.AUTH_REISSUE.message,
                result = result.tokenRes
            ), HttpStatus.OK
        )
    }

    @PutMapping("/password")
    fun changePassword(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @CookieValue(name = "accessToken", required = false) accessToken: String?,
        @RequestBody req: ChangePasswordReq,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Nothing?>> {
        authService.changePassword(userDetails.username, req, accessToken)
        response.addHeader(HttpHeaders.SET_COOKIE, expireAccessTokenCookie())
        response.addHeader(HttpHeaders.SET_COOKIE, expireRefreshTokenCookie())

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_CHANGE_PASSWORD.code,
                message = ResponseCode.AUTH_CHANGE_PASSWORD.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @CookieValue(name = "accessToken", required = false) accessToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "logout: ${userDetails.username}" }

        authService.logout(userDetails.username, accessToken)
        response.addHeader(HttpHeaders.SET_COOKIE, expireAccessTokenCookie())
        response.addHeader(HttpHeaders.SET_COOKIE, expireRefreshTokenCookie())

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_LOGOUT.code,
                message = ResponseCode.AUTH_LOGOUT.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("/profile")
    fun getProfile(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<ApiResponse<MemberRes>> {
        val profile = authService.getProfile(userDetails.username)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_PROFILE.code,
                message = ResponseCode.AUTH_PROFILE.message,
                result = profile
            ), HttpStatus.OK
        )
    }

    @PutMapping("/profile")
    fun updateProfile(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestBody req: UpdateProfileReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        authService.updateProfile(userDetails.username, req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_PROFILE_UPDATE.code,
                message = ResponseCode.AUTH_PROFILE_UPDATE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("/api-keys")
    fun getApiKeys(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<ApiResponse<List<ApiKeyRes>>> {
        val apiKeys = authService.getApiKeys(userDetails.username)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_API_KEY_LIST.code,
                message = ResponseCode.AUTH_API_KEY_LIST.message,
                result = apiKeys
            ), HttpStatus.OK
        )
    }

    @PostMapping("/api-keys")
    fun createApiKey(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestBody req: ApiKeyReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        authService.createApiKey(userDetails.username, req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_API_KEY_CREATE.code,
                message = ResponseCode.AUTH_API_KEY_CREATE.message,
                result = null
            ), HttpStatus.CREATED
        )
    }

    @DeleteMapping("/api-keys/{id}")
    fun deleteApiKey(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        authService.deleteApiKey(userDetails.username, id)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_API_KEY_DELETE.code,
                message = ResponseCode.AUTH_API_KEY_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PostMapping("/admin/members")
    @PreAuthorize("hasRole('ADMIN')")
    fun createMember(@RequestBody req: CreateMemberReq): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "create member: ${req.loginId}" }

        authService.createMember(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_CREATE_MEMBER.code,
                message = ResponseCode.AUTH_CREATE_MEMBER.message,
                result = null
            ), HttpStatus.CREATED
        )
    }

    @GetMapping("/admin/members")
    @PreAuthorize("hasRole('ADMIN')")
    fun getMembers(): ResponseEntity<ApiResponse<List<MemberRes>>> {
        val members = authService.getMembers()

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_MEMBER_LIST.code,
                message = ResponseCode.AUTH_MEMBER_LIST.message,
                result = members
            ), HttpStatus.OK
        )
    }

    @PutMapping("/admin/members/{loginId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    fun changeRole(
        @PathVariable loginId: String,
        @RequestBody req: ChangeRoleReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "change role: $loginId -> ${req.role}" }

        authService.changeRole(loginId, req.role)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_CHANGE_ROLE.code,
                message = ResponseCode.AUTH_CHANGE_ROLE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PutMapping("/admin/members/{loginId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    fun lockAccount(
        @PathVariable loginId: String
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "lock account: $loginId" }

        authService.lockAccount(loginId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_LOCK.code,
                message = ResponseCode.AUTH_LOCK.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PutMapping("/admin/members/{loginId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    fun unlockAccount(
        @PathVariable loginId: String
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "unlock account: $loginId" }

        authService.unlockAccount(loginId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_UNLOCK.code,
                message = ResponseCode.AUTH_UNLOCK.message,
                result = null
            ), HttpStatus.OK
        )
    }

    private fun createAccessTokenCookie(accessToken: String): String =
        ResponseCookie.from("accessToken", accessToken)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Strict")
            .path("/")
            .maxAge(accessTokenExpiration * 60)
            .build()
            .toString()

    private fun expireAccessTokenCookie(): String =
        ResponseCookie.from("accessToken", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Strict")
            .path("/")
            .maxAge(0)
            .build()
            .toString()

    private fun createRefreshTokenCookie(refreshToken: String): String =
        ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(refreshTokenExpiration * 24 * 60 * 60)
            .build()
            .toString()

    private fun expireRefreshTokenCookie(): String =
        ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(0)
            .build()
            .toString()
}
