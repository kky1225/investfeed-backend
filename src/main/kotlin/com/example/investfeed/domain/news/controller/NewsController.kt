package com.example.investfeed.domain.news.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.news.dto.req.NewsSearchReq
import com.example.investfeed.domain.news.dto.res.NewsListRes
import com.example.investfeed.domain.news.service.NewsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/news")
class NewsController(
    private val newsService: NewsService
) {

    @PostMapping("search")
    fun search(
        @RequestBody req: NewsSearchReq
    ): ResponseEntity<ApiResponse<NewsListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.NEWS_SEARCH.code,
                message = ResponseCode.NEWS_SEARCH.message,
                result = newsService.searchNews(req.query, req.page)
            ), HttpStatus.OK
        )
    }
}
