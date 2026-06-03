package com.example.investfeed.domain.auth.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.auth.dto.req.*
import com.example.investfeed.domain.auth.dto.res.*
import com.example.investfeed.domain.auth.exception.PreAuthTokenMissingException
import com.example.investfeed.domain.auth.service.AuthService
import com.example.investfeed.domain.security.CustomUserDetails
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

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
    ): ResponseEntity<ApiResponse<PreAuthRes>> {
        log.info { "login: ${req.loginId}" }

        val result = authService.login(req)
        response.addHeader(HttpHeaders.SET_COOKIE, createPreAuthTokenCookie(result.preAuthToken))

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_TOTP_REQUIRED.code,
                message = ResponseCode.AUTH_TOTP_REQUIRED.message,
                result = result.preAuthRes
            ), HttpStatus.OK
        )
    }

    @PostMapping("/totp/setup")
    fun totpSetup(
        @CookieValue(name = "preAuthToken", required = false) preAuthToken: String?
    ): ResponseEntity<ApiResponse<TotpSetupRes>> {
        if (preAuthToken == null) throw com.example.investfeed.domain.auth.exception.PreAuthTokenMissingException()

        val result = authService.totpSetup(preAuthToken)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_TOTP_SETUP.code,
                message = ResponseCode.AUTH_TOTP_SETUP.message,
                result = result
            ), HttpStatus.OK
        )
    }

    @PostMapping("/totp/verify")
    fun totpVerify(
        @CookieValue(name = "preAuthToken", required = false) preAuthToken: String?,
        @RequestBody req: TotpVerifyReq,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<TokenRes>> {
        if (preAuthToken == null) throw PreAuthTokenMissingException()

        val result = authService.totpVerify(preAuthToken, req.code)
        response.addHeader(HttpHeaders.SET_COOKIE, expirePreAuthTokenCookie())
        response.addHeader(HttpHeaders.SET_COOKIE, createAccessTokenCookie(result.accessToken))
        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(result.refreshToken))

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_TOTP_VERIFY.code,
                message = ResponseCode.AUTH_TOTP_VERIFY.message,
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
        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(result.refreshToken))

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_REISSUE.code,
                message = ResponseCode.AUTH_REISSUE.message,
                result = result.tokenRes
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/password")
    fun changePassword(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @CookieValue(name = "accessToken", required = false) accessToken: String?,
        @Valid @RequestBody req: ChangePasswordReq,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Nothing?>> {
        authService.changePassword(userDetails.username, req, accessToken)
        response.addHeader(HttpHeaders.SET_COOKIE, expireAccessTokenCookie())
        response.addHeader(HttpHeaders.SET_COOKIE, expireRefreshTokenCookie())
        response.addHeader(HttpHeaders.SET_COOKIE, expireSecondaryAuthCookie())

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
        @AuthenticationPrincipal userDetails: CustomUserDetails?,
        @CookieValue(name = "accessToken", required = false) accessToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Nothing?>> {
        userDetails?.username?.let { username ->
            log.info { "logout: $username" }
            authService.logout(username, accessToken)
        }

        response.addHeader(HttpHeaders.SET_COOKIE, expireAccessTokenCookie())
        response.addHeader(HttpHeaders.SET_COOKIE, expireRefreshTokenCookie())
        response.addHeader(HttpHeaders.SET_COOKIE, expireSecondaryAuthCookie())

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
        @Valid @RequestBody req: UpdateProfileReq
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
        @Valid @RequestBody req: ApiKeyReq
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

    @PostMapping("/secondary-password/setup")
    fun setupSecondaryPassword(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestBody req: SecondaryPasswordSetupReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        authService.setupSecondaryPassword(userDetails.username, req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_SECONDARY_SETUP.code,
                message = ResponseCode.AUTH_SECONDARY_SETUP.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("/secondary-password/lock-status")
    fun getSecondaryPasswordLockStatus(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<ApiResponse<SecondaryPasswordLockStatusRes>> {
        val remainingSeconds = authService.getSecondaryPasswordLockStatus(userDetails.username)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_SECONDARY_LOCK_STATUS.code,
                message = ResponseCode.AUTH_SECONDARY_LOCK_STATUS.message,
                result = SecondaryPasswordLockStatusRes(remainingSeconds)
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/secondary-password/change")
    fun changeSecondaryPassword(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestBody req: SecondaryPasswordChangeReq,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Nothing?>> {
        authService.changeSecondaryPassword(userDetails.username, req)
        response.addHeader(HttpHeaders.SET_COOKIE, expireSecondaryAuthCookie())

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_SECONDARY_CHANGE.code,
                message = ResponseCode.AUTH_SECONDARY_CHANGE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PostMapping("/secondary-password/verify")
    fun verifySecondaryPassword(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestBody req: SecondaryPasswordVerifyReq,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Nothing?>> {
        val token = authService.verifySecondaryPassword(userDetails.username, req)
        response.addHeader(HttpHeaders.SET_COOKIE, createSecondaryAuthCookie(token))

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_SECONDARY_VERIFY.code,
                message = ResponseCode.AUTH_SECONDARY_VERIFY.message,
                result = null
            ), HttpStatus.OK
        )
    }

    private fun createPreAuthTokenCookie(preAuthToken: String): String =
        ResponseCookie.from("preAuthToken", preAuthToken)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(5 * 60)
            .build()
            .toString()

    private fun expirePreAuthTokenCookie(): String =
        ResponseCookie.from("preAuthToken", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(0)
            .build()
            .toString()

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

    private fun createSecondaryAuthCookie(token: String): String =
        ResponseCookie.from("secondaryAuthToken", token)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Strict")
            .path("/")
            .maxAge(30 * 60)
            .build()
            .toString()

    private fun expireSecondaryAuthCookie(): String =
        ResponseCookie.from("secondaryAuthToken", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Strict")
            .path("/")
            .maxAge(0)
            .build()
            .toString()
}
