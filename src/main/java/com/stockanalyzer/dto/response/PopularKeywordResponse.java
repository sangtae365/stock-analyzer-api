package com.stockanalyzer.dto.response;

import java.util.List;

public record PopularKeywordResponse(List<KeywordDto> keywords) {
    public record KeywordDto(int rank, String keyword, int searchCount) {}
}
