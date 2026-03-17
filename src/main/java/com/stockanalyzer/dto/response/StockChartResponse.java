package com.stockanalyzer.dto.response;

import java.util.List;

public record StockChartResponse(
    String ticker,
    String name,
    String period,          // D:일봉 W:주봉 M:월봉 Y:년봉
    Summary summary,
    List<Candle> candles
) {
    public record Summary(
        long   currentPrice,
        double changeRate,
        long   changeAmount,
        long   volume,
        long   marketCapBil,
        double per,
        double pbr,
        double eps,
        long   listedShares,
        long   capital,
        long   faceValue,
        double volumeTurnover
    ) {}

    public record Candle(
        String date,       // YYYYMMDD
        long   open,
        long   high,
        long   low,
        long   close,
        long   volume,
        long   amount,     // 거래대금
        String exDivCode   // 락 구분 (01:권리락, 02:배당락...)
    ) {}
}
