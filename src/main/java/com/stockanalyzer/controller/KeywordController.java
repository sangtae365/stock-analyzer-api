package com.stockanalyzer.controller;

import com.stockanalyzer.dto.response.PopularKeywordResponse;
import com.stockanalyzer.service.KeywordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/keywords")
public class KeywordController {

    private final KeywordService keywordService;

    public KeywordController(KeywordService keywordService) {
        this.keywordService = keywordService;
    }

    @GetMapping("/popular")
    public ResponseEntity<PopularKeywordResponse> getPopularKeywords(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(keywordService.getPopularKeywords(limit));
    }
}
