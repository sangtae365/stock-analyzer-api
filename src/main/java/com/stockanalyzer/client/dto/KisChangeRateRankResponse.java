package com.stockanalyzer.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * KIS FHPST01700000 - 등락률순위 응답
 * GET /uapi/domestic-stock/v1/ranking/fluctuation
 * 실전만 지원, 최대 30건
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisChangeRateRankResponse(
    @JsonProperty("rt_cd")   String resultCode,
    @JsonProperty("msg_cd")  String messageCode,
    @JsonProperty("msg1")    String message,
    @JsonProperty("output")  List<Output> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output(
        @JsonProperty("data_rank")                    String rank,           // 순위
        @JsonProperty("hts_kor_isnm")                 String name,           // 종목명
        @JsonProperty("stck_shrn_iscd")               String ticker,         // 종목코드
        @JsonProperty("stck_prpr")                    String currentPrice,   // 현재가
        @JsonProperty("prdy_vrss_sign")               String changeSign,     // 전일 대비 부호
        @JsonProperty("prdy_vrss")                    String changeAmount,   // 전일 대비
        @JsonProperty("prdy_ctrt")                    String changeRate,     // 전일 대비율
        @JsonProperty("acml_vol")                     String volume,         // 누적 거래량
        @JsonProperty("stck_hgpr")                    String highPrice,      // 최고가
        @JsonProperty("stck_lwpr")                    String lowPrice,       // 최저가
        @JsonProperty("cnnt_ascn_dynu")               String consecutiveRisedays,  // 연속 상승 일수
        @JsonProperty("cnnt_down_dynu")               String consecutiveFallDays,  // 연속 하락 일수
        @JsonProperty("hgpr_vrss_prpr_rate")          String fromHighRate,   // 최고가 대비 현재가 비율
        @JsonProperty("lwpr_vrss_prpr_rate")          String fromLowRate,    // 최저가 대비 현재가 비율
        @JsonProperty("oprc_vrss_prpr_rate")          String fromOpenRate,   // 시가 대비 현재가 비율
        @JsonProperty("prd_rsfl_rate")                String periodChangeRate // 기간 등락 비율
    ) {}

    public boolean isSuccess() { return "0".equals(resultCode); }
}
