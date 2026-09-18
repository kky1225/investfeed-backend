package com.example.investfeed.domain.assistant.controller

import com.example.investfeed.domain.assistant.service.AssistantTokenService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/assistant/.well-known")
class AssistantJwksController(
    private val assistantTokenService: AssistantTokenService,
) {
    @GetMapping("jwks.json")
    fun jwks(): Map<String, Any> = assistantTokenService.jwks()
}
