package com.stockanalyzer.dto.response;

import com.stockanalyzer.dto.StockDto;

import java.util.List;

public record StockListResponse(
    int totalCount,
    int page,
    int size,
    List<StockDto> stocks
) {}
