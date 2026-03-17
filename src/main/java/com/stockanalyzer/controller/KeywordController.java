package com.stockanalyzer.controller;

import com.stockanalyzer.dto.response.PopularKeywordResponse;
import com.stockanalyzer.service.KeywordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "키워드", description = "인기 키워드 조회")
@RestController
@RequestMapping("/api/keywords")
public class KeywordController {

    private final KeywordService keywordService;

    public KeywordController(KeywordService keywordService) {
        this.keywordService = keywordService;
    }

    @Operation(summary = "인기 키워드 목록 조회", description = "현재 인기 있는 주식 키워드 목록을 반환합니다.")
    @GetMapping("/popular")
    public ResponseEntity<PopularKeywordResponse> getPopularKeywords(
            @Parameter(description = "조회할 키워드 수") @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(keywordService.getPopularKeywords(limit));
    }
}
