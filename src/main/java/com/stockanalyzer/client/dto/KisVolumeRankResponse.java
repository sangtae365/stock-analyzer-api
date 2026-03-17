package com.stockanalyzer.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * KIS FHPST01710000 - 거래량순위 응답
 * GET /uapi/domestic-stock/v1/quotations/volume-rank
 * 실전만 지원, 최대 30건
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisVolumeRankResponse(
    @JsonProperty("rt_cd")   String resultCode,
    @JsonProperty("msg_cd")  String messageCode,
    @JsonProperty("msg1")    String message,
    @JsonProperty("output")  List<Output> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output(
        @JsonProperty("data_rank")          String rank,             // 순위
        @JsonProperty("hts_kor_isnm")       String name,             // 종목명
        @JsonProperty("mksc_shrn_iscd")     String ticker,           // 종목코드
        @JsonProperty("stck_prpr")          String currentPrice,     // 현재가
        @JsonProperty("prdy_vrss_sign")     String changeSign,       // 전일 대비 부호
        @JsonProperty("prdy_vrss")          String changeAmount,     // 전일 대비
        @JsonProperty("prdy_ctrt")          String changeRate,       // 전일 대비율
        @JsonProperty("acml_vol")           String volume,           // 누적 거래량
        @JsonProperty("prdy_vol")           String prevVolume,       // 전일 거래량
        @JsonProperty("vol_inrt")           String volumeIncRate,    // 거래량증가율
        @JsonProperty("vol_tnrt")           String volumeTurnover,   // 거래량 회전율
        @JsonProperty("avrg_vol")           String avgVolume,        // 평균 거래량
        @JsonProperty("avrg_tr_pbmn")       String avgAmount,        // 평균 거래 대금
        @JsonProperty("acml_tr_pbmn")       String totalAmount,      // 누적 거래 대금
        @JsonProperty("lstn_stcn")          String listedShares      // 상장 주수
    ) {}

    public boolean isSuccess() { return "0".equals(resultCode); }
}
