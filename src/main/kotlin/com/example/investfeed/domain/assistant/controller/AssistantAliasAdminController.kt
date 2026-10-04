package com.example.investfeed.domain.assistant.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.assistant.entity.AssistantStockAlias
import com.example.investfeed.domain.assistant.repository.AssistantStockAliasRepository
import com.example.investfeed.internal.assistant.tool.StockMarket
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequiresAction(permission = Permissions.ADMIN_MONITORING)
@RestController
@RequestMapping("/api/admin/assistant/aliases")
class AssistantAliasAdminController(
    private val aliasRepository: AssistantStockAliasRepository,
) {
    data class AliasRes(val id: Long, val alias: String, val market: StockMarket, val stkCd: String, val createdAt: LocalDateTime) {
        companion object {
            fun from(e: AssistantStockAlias) = AliasRes(e.id, e.alias, StockMarket.valueOf(e.market), e.stkCd, e.createdAt)
        }
    }

    data class AliasCreateReq(
        @field:NotBlank val alias: String,
        val market: StockMarket,
        @field:NotBlank val stkCd: String,
    )

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun list(): ResponseEntity<ApiResponse<List<AliasRes>>> =
        ok(ResponseCode.ASSISTANT_ALIAS_LIST, aliasRepository.findAllByOrderByAliasAsc().map { AliasRes.from(it) })

    @PostMapping
    @RequiresAction(action = Actions.CREATE)
    fun create(@Valid @RequestBody req: AliasCreateReq): ResponseEntity<ApiResponse<AliasRes>> {
        val saved = aliasRepository.save(AssistantStockAlias(alias = req.alias.trim(), market = req.market.name, stkCd = req.stkCd.trim().uppercase()))
        return ok(ResponseCode.ASSISTANT_ALIAS_CREATE, AliasRes.from(saved))
    }

    @DeleteMapping("{id}")
    @RequiresAction(action = Actions.DELETE)
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<Long>> {
        aliasRepository.deleteById(id)
        return ok(ResponseCode.ASSISTANT_ALIAS_DELETE, id)
    }

    private fun <T> ok(code: ResponseCode, result: T): ResponseEntity<ApiResponse<T>> =
        ResponseEntity(ApiResponse(code = code.code, message = code.message, result = result), HttpStatus.OK)
}
