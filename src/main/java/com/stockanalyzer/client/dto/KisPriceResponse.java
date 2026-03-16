package com.stockanalyzer.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KisPriceResponse(
    @JsonProperty("rt_cd")  String resultCode,   // "0" = 정상
    @JsonProperty("msg1")   String message,
    @JsonProperty("output") Output output
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output(
        @JsonProperty("hts_kor_isnm")   String name,          // 종목명
        @JsonProperty("stck_shrn_iscd") String ticker,        // 종목코드
        @JsonProperty("stck_prpr")      String currentPrice,  // 현재가
        @JsonProperty("prdy_vrss")      String changeAmount,  // 전일 대비
        @JsonProperty("prdy_ctrt")      String changeRate,    // 등락률 (%)
        @JsonProperty("hts_avls")       String marketCapBil   // 시가총액 (억)
    ) {}

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}
