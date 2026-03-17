package com.stockanalyzer.service;

import com.stockanalyzer.dto.response.PopularKeywordResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeywordService {

    public PopularKeywordResponse getPopularKeywords(int limit) {
        return new PopularKeywordResponse(List.of());
    }
}
