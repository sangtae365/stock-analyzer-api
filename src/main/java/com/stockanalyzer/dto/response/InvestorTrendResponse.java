package com.stockanalyzer.dto.response;

import java.util.List;

public record InvestorTrendResponse(
    String ticker,
    String date,           // 조회 기준일 (YYYYMMDD)
    Summary summary,
    List<DailyData> dailyData
) {
    public record Summary(
        long   currentPrice,
        double changeRate,
        long   volume
    ) {}

    /** 일별 투자자별 순매수 데이터. qty=수량(주), amt=금액(백만원) */
    public record DailyData(
        String date,            // YYYYMMDD
        long   closePrice,
        long   volume,
        long   amount,          // 거래대금 (백만원)

        // 순매수 수량
        long foreignNetQty,
        long personNetQty,
        long instNetQty,
        long securitiesNetQty,
        long trustNetQty,
        long peFundNetQty,
        long bankNetQty,
        long insuranceNetQty,
        long pensionNetQty,
        long etcCorpNetQty,

        // 순매수 금액 (백만원)
        long foreignNetAmt,
        long personNetAmt,
        long instNetAmt,
        long securitiesNetAmt,
        long trustNetAmt,
        long peFundNetAmt,
        long bankNetAmt,
        long insuranceNetAmt,
        long pensionNetAmt,
        long etcCorpNetAmt
    ) {}
}
