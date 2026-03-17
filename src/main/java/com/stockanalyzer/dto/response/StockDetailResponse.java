package com.stockanalyzer.dto.response;

public record StockDetailResponse(
    // 기본 정보
    String ticker,
    String name,
    String sector,
    String marketName,         // 대표시장 (KOSPI, KOSDAQ ...)

    // 실시간 시세
    long   currentPrice,
    long   changeAmount,       // 전일 대비 (원)
    double changeRate,         // 전일 대비율 (%)
    String changeSign,         // 1:상한 2:상승 3:보합 4:하락 5:하한
    long   openPrice,
    long   highPrice,
    long   lowPrice,
    long   upperLimitPrice,    // 상한가
    long   lowerLimitPrice,    // 하한가
    long   volume,             // 누적 거래량
    long   amount,             // 누적 거래 대금

    // 시가총액 / 기업 지표
    long   marketCap,          // 시가총액 (원)
    double per,
    double pbr,
    double eps,
    double bps,
    long   listedShares,       // 상장 주수
    long   faceValue,          // 액면가
    String fiscalMonth,        // 결산 월 (MM)
    double volumeTurnover,     // 거래량 회전율

    // 52주 / 연중 고저가
    long   w52HighPrice,
    String w52HighPriceDate,
    double w52HighPriceRate,   // 52주 최고가 대비 현재가 (%)
    long   w52LowPrice,
    String w52LowPriceDate,
    double w52LowPriceRate,

    long   yearHighPrice,
    String yearHighPriceDate,
    double yearHighPriceRate,
    long   yearLowPrice,
    String yearLowPriceDate,
    double yearLowPriceRate,

    // 외국인
    double foreignExhaustionRate, // 외국인 소진율 (%)
    long   foreignNetBuyQty,      // 외국인 순매수 수량

    // 융자
    double loanRatio,          // 전체 융자 잔고 비율

    // 종목 상태
    String statusCode,         // 종목 상태 코드
    String marketWarnCode,     // 시장경고코드
    String viCode,             // VI 적용 구분
    boolean shortSellAvailable,   // 공매도 가능 여부

    // 피벗 포인트
    double pivotPoint,
    double pivot1Resist,       // 1차 저항
    double pivot1Support       // 1차 지지
) {}
