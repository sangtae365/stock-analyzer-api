package com.stockanalyzer.dto;

public record StockDto(
    String ticker,
    String name,
    long price,
    double changeRate,
    long marketCap,
    String sector
) {}
