package com.stockanalyzer.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record StockAnalysisResponse(
    String keyword,
    LocalDateTime analyzedAt,
    int totalCount,
    List<CategoryDto> categories
) {
    public record CategoryDto(
        String category,
        List<AnalyzedStockDto> stocks
    ) {}

    public record AnalyzedStockDto(
        String ticker,
        String name,
        long price,
        double changeRate,
        String summary
    ) {}
}
