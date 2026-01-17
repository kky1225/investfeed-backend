package com.example.investfeed.domain.sect.controller

import com.example.investfeed.domain.sect.dto.req.SectListReq
import com.example.investfeed.domain.sect.dto.res.SectListRes
import com.example.investfeed.domain.sect.service.SectService
import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sect")
class SectController(
    private val sectService: SectService
) {
    @GetMapping("list")
    fun sectIndexList(
        req: SectListReq
    ): ResponseEntity<ApiResponse<SectListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_LIST.code,
                message = ResponseCode.SECT_LIST.message,
                result = sectService.sectIndexList(req = req)
            ), HttpStatus.OK
        )
    }
}