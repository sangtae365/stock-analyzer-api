package com.stockanalyzer.dto.response;

import java.util.List;

public record StockDetailResponse(
    String ticker,
    String name,
    long price,
    double changeRate,
    long marketCap,
    String sector,
    String aiAnalysis,
    List<String> relatedKeywords
) {}
