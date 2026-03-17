package com.stockanalyzer.dto.response;

import java.util.List;

public record StockRankingResponse(
    String rankType,   // "volume" | "changeRate"
    String sortBy,     // 정렬 기준 설명
    List<RankedStock> stocks
) {
    public record RankedStock(
        int    rank,
        String ticker,
        String name,
        long   currentPrice,
        double changeRate,
        long   changeAmount,
        long   volume,
        long   totalAmount,    // 누적 거래 대금 (거래량 순위)
        double volumeIncRate,  // 거래량 증가율 (거래량 순위)
        int    consecutiveRiseDays,  // 연속 상승 일수 (등락률 순위)
        int    consecutiveFallDays   // 연속 하락 일수 (등락률 순위)
    ) {}
}
