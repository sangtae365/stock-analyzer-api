package com.stockanalyzer.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * KIS FHPST01740000 - 시가총액 순위 응답
 * GET /uapi/domestic-stock/v1/ranking/market-cap
 * 최대 30건
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisMarketCapRankResponse(
    @JsonProperty("rt_cd")   String resultCode,
    @JsonProperty("msg_cd")  String messageCode,
    @JsonProperty("msg1")    String message,
    @JsonProperty("output")  List<Output> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output(
        @JsonProperty("mksc_shrn_iscd")      String ticker,          // 종목코드
        @JsonProperty("data_rank")           String rank,            // 순위
        @JsonProperty("hts_kor_isnm")        String name,            // 종목명
        @JsonProperty("stck_prpr")           String currentPrice,    // 현재가
        @JsonProperty("prdy_vrss_sign")      String changeSign,      // 전일 대비 부호
        @JsonProperty("prdy_vrss")           String changeAmount,    // 전일 대비
        @JsonProperty("prdy_ctrt")           String changeRate,      // 전일 대비율
        @JsonProperty("acml_vol")            String volume,          // 누적 거래량
        @JsonProperty("lstn_stcn")           String listedShares,    // 상장 주수
        @JsonProperty("stck_avls")           String marketCap,       // 시가총액 (억원)
        @JsonProperty("mrkt_whol_avls_rlim") String marketCapWeight  // 시장 전체 시가총액 비중 (%)
    ) {}

    public boolean isSuccess() { return "0".equals(resultCode); }
}
