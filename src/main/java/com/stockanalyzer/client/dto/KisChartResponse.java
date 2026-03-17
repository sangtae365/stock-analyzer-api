package com.stockanalyzer.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * KIS FHKST03010100 - 국내주식기간별시세(일/주/월/년) 응답
 * GET /uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisChartResponse(
    @JsonProperty("rt_cd")   String resultCode,
    @JsonProperty("msg_cd")  String messageCode,
    @JsonProperty("msg1")    String message,
    @JsonProperty("output1") Output1 summary,
    @JsonProperty("output2") List<Output2> candles
) {
    /** 현재 종목 요약 정보 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output1(
        @JsonProperty("hts_kor_isnm")   String name,           // HTS 한글 종목명
        @JsonProperty("stck_shrn_iscd") String ticker,         // 단축 종목코드
        @JsonProperty("stck_prpr")      String currentPrice,   // 현재가
        @JsonProperty("prdy_vrss")      String changeAmount,   // 전일 대비
        @JsonProperty("prdy_vrss_sign") String changeSign,     // 전일 대비 부호
        @JsonProperty("prdy_ctrt")      String changeRate,     // 전일 대비율
        @JsonProperty("acml_vol")       String volume,         // 누적 거래량
        @JsonProperty("acml_tr_pbmn")   String amount,         // 누적 거래 대금
        @JsonProperty("stck_oprc")      String openPrice,      // 시가
        @JsonProperty("stck_hgpr")      String highPrice,      // 최고가
        @JsonProperty("stck_lwpr")      String lowPrice,       // 최저가
        @JsonProperty("stck_mxpr")      String upperLimit,     // 상한가
        @JsonProperty("stck_llam")      String lowerLimit,     // 하한가
        @JsonProperty("hts_avls")       String marketCapBil,   // 시가총액 (억)
        @JsonProperty("per")            String per,
        @JsonProperty("pbr")            String pbr,
        @JsonProperty("eps")            String eps,
        @JsonProperty("lstn_stcn")      String listedShares,   // 상장 주수
        @JsonProperty("cpfn")           String capital,        // 자본금
        @JsonProperty("stck_fcam")      String faceValue,      // 액면가
        @JsonProperty("vol_tnrt")       String volumeTurnover  // 거래량 회전율
    ) {}

    /** 기간별 OHLCV 캔들 데이터 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output2(
        @JsonProperty("stck_bsop_date") String date,       // 영업 일자 (YYYYMMDD)
        @JsonProperty("stck_clpr")      String closePrice, // 종가
        @JsonProperty("stck_oprc")      String openPrice,  // 시가
        @JsonProperty("stck_hgpr")      String highPrice,  // 최고가
        @JsonProperty("stck_lwpr")      String lowPrice,   // 최저가
        @JsonProperty("acml_vol")       String volume,     // 거래량
        @JsonProperty("acml_tr_pbmn")   String amount,     // 거래 대금
        @JsonProperty("prdy_vrss_sign") String changeSign, // 전일 대비 부호
        @JsonProperty("prdy_vrss")      String changeAmount, // 전일 대비
        @JsonProperty("flng_cls_code")  String exDivCode,  // 락 구분 코드 (01:권리락 02:배당락 ...)
        @JsonProperty("mod_yn")         String modYn       // 변경 여부 (Y:시가 없음)
    ) {}

    public boolean isSuccess() { return "0".equals(resultCode); }
}
