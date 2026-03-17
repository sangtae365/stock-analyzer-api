package com.stockanalyzer.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KisPriceResponse(
    @JsonProperty("rt_cd")  String resultCode,   // "0" = 정상
    @JsonProperty("msg_cd") String messageCode,
    @JsonProperty("msg1")   String message,
    @JsonProperty("output") Output output
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output(
        // 스펙 명시 필드
        @JsonProperty("stck_shrn_iscd") String ticker,           // 주식 단축 종목코드
        @JsonProperty("stck_prpr")      String currentPrice,     // 주식 현재가
        @JsonProperty("prdy_vrss")      String changeAmount,     // 전일 대비
        @JsonProperty("prdy_vrss_sign") String changeSign,       // 전일 대비 부호 (1:상한 2:상승 3:보합 4:하락 5:하한)
        @JsonProperty("prdy_ctrt")      String changeRate,       // 전일 등락률 (%)
        @JsonProperty("acml_vol")       String accumulatedVolume,// 누적 거래량
        @JsonProperty("acml_tr_pbmn")   String accumulatedAmount,// 누적 거래 대금
        @JsonProperty("stck_oprc")      String openPrice,        // 주식 시가
        @JsonProperty("stck_hgpr")      String highPrice,        // 주식 최고가
        @JsonProperty("stck_lwpr")      String lowPrice,         // 주식 최저가
        @JsonProperty("stck_mxpr")      String upperLimitPrice,  // 주식 상한가
        @JsonProperty("stck_llam")      String lowerLimitPrice,  // 주식 하한가
        @JsonProperty("hts_avls")       String marketCapBil,     // HTS 시가총액 (억)
        @JsonProperty("per")            String per,              // PER
        @JsonProperty("pbr")            String pbr,              // PBR
        @JsonProperty("eps")            String eps,              // EPS
        @JsonProperty("bps")            String bps,              // BPS
        @JsonProperty("bstp_kor_isnm")  String sectorName,       // 업종 한글 종목명 (스펙 명시)
        @JsonProperty("rprs_mrkt_kor_name") String marketName,   // 대표 시장 한글 명 (ex: KOSPI200)

        // 스펙 미명시지만 실제 응답에 포함되는 종목명 필드 (fallback용)
        @JsonProperty("hts_kor_isnm")   String htsName,          // HTS 한글 종목명

        // 52주 / 연중 / 250일 고저가
        @JsonProperty("w52_hgpr")                  String w52HighPrice,         // 52주 최고가
        @JsonProperty("w52_hgpr_date")             String w52HighPriceDate,     // 52주 최고가 일자
        @JsonProperty("w52_hgpr_vrss_prpr_ctrt")   String w52HighPriceRate,     // 52주 최고가 대비 현재가
        @JsonProperty("w52_lwpr")                  String w52LowPrice,          // 52주 최저가
        @JsonProperty("w52_lwpr_date")             String w52LowPriceDate,      // 52주 최저가 일자
        @JsonProperty("w52_lwpr_vrss_prpr_ctrt")   String w52LowPriceRate,      // 52주 최저가 대비 현재가
        @JsonProperty("stck_dryy_hgpr")            String yearHighPrice,        // 연중 최고가
        @JsonProperty("dryy_hgpr_date")            String yearHighPriceDate,    // 연중 최고가 일자
        @JsonProperty("dryy_hgpr_vrss_prpr_rate")  String yearHighPriceRate,    // 연중 최고가 대비 현재가
        @JsonProperty("stck_dryy_lwpr")            String yearLowPrice,         // 연중 최저가
        @JsonProperty("dryy_lwpr_date")            String yearLowPriceDate,     // 연중 최저가 일자
        @JsonProperty("dryy_lwpr_vrss_prpr_rate")  String yearLowPriceRate,     // 연중 최저가 대비 현재가

        // 외국인 / 공매도
        @JsonProperty("hts_frgn_ehrt")  String foreignExhaustionRate, // 외국인 소진율 (%)
        @JsonProperty("frgn_ntby_qty")  String foreignNetBuyQty,      // 외국인 순매수 수량

        // 종목 기본 정보
        @JsonProperty("lstn_stcn")      String listedShares,     // 상장 주수
        @JsonProperty("stck_fcam")      String faceValue,        // 주식 액면가
        @JsonProperty("aspr_unit")      String tickSize,         // 호가단위
        @JsonProperty("stac_month")     String fiscalMonth,      // 결산 월
        @JsonProperty("vol_tnrt")       String volumeTurnover,   // 거래량 회전율
        @JsonProperty("whol_loan_rmnd_rate") String loanRatio,   // 전체 융자 잔고 비율

        // 종목 상태
        @JsonProperty("iscd_stat_cls_code") String statusCode,   // 종목 상태 (51:관리 52:투자위험 53:경고 54:주의 55:신용가능 57:증거금100% 58:거래정지 59:단기과열)
        @JsonProperty("mrkt_warn_cls_code") String marketWarnCode, // 시장경고코드
        @JsonProperty("vi_cls_code")        String viCode,       // VI적용구분코드
        @JsonProperty("sltr_yn")            String cleanupTradeYn, // 정리매매여부
        @JsonProperty("short_over_yn")      String shortOverYn,  // 단기과열여부
        @JsonProperty("ssts_yn")            String shortSellYn,  // 공매도가능여부

        // 피벗 포인트
        @JsonProperty("pvt_pont_val")    String pivotPoint,      // 피벗 포인트
        @JsonProperty("pvt_frst_dmrs_prc") String pivot1Resist,  // 피벗 1차 저항
        @JsonProperty("pvt_frst_dmsp_prc") String pivot1Support  // 피벗 1차 지지
    ) {}

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}
