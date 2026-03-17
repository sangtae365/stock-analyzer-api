package com.stockanalyzer.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * KIS CTPF1002R - 주식기본조회 응답
 * GET /uapi/domestic-stock/v1/quotations/search-stock-info
 * 실전만 지원 (모의 미지원)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisStockInfoResponse(
    @JsonProperty("rt_cd")   String resultCode,
    @JsonProperty("msg_cd")  String messageCode,
    @JsonProperty("msg1")    String message,
    @JsonProperty("output")  Output output
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output(
        @JsonProperty("pdno")              String ticker,          // 종목번호
        @JsonProperty("prdt_name")         String name,            // 상품명 (풀네임)
        @JsonProperty("prdt_abrv_name")    String shortName,       // 상품약어명
        @JsonProperty("prdt_eng_name")     String engName,         // 영문명
        @JsonProperty("std_pdno")          String isin,            // 표준상품번호 (KR7XXXXXXXX01)
        @JsonProperty("mket_id_cd")        String marketCode,      // 시장ID코드 (STK:유가증권, KSQ:코스닥, KNX:코넥스)
        @JsonProperty("scty_grp_id_cd")    String securityGroup,   // 증권그룹 (ST:주권, EF:ETF, EN:ETN, EW:ELW)
        @JsonProperty("stck_kind_cd")      String stockKind,       // 주식종류코드 (101:보통주, 201:우선주...)
        @JsonProperty("setl_mmdd")         String fiscalMonth,     // 결산월일 (MMDD)
        @JsonProperty("lstg_stqt")         String listedShares,    // 상장주수
        @JsonProperty("lstg_cptl_amt")     String listedCapital,   // 상장자본금
        @JsonProperty("cpta")              String capital,         // 자본금
        @JsonProperty("papr")              String faceValue,       // 액면가
        @JsonProperty("issu_pric")         String issuePrice,      // 발행가격
        @JsonProperty("kospi200_item_yn")  String kospi200Yn,      // 코스피200종목여부
        @JsonProperty("scts_mket_lstg_dt") String kospiListingDate,  // 유가증권시장 상장일
        @JsonProperty("kosdaq_mket_lstg_dt") String kosdaqListingDate, // 코스닥시장 상장일
        @JsonProperty("std_idst_clsf_cd")      String industryCode,    // 표준산업분류코드
        @JsonProperty("std_idst_clsf_cd_name") String industryName,    // 표준산업분류코드명 (예: "반도체 제조업")
        @JsonProperty("idx_bztp_lcls_cd_name") String sectorLargeName, // 지수업종대분류명
        @JsonProperty("idx_bztp_mcls_cd_name") String sectorMidName,   // 지수업종중분류명
        @JsonProperty("tr_stop_yn")        String tradeStopYn,     // 거래정지여부
        @JsonProperty("admn_item_yn")      String adminItemYn,     // 관리종목여부
        @JsonProperty("thdt_clpr")         String todayClosePrice, // 당일종가
        @JsonProperty("bfdy_clpr")         String prevClosePrice,  // 전일종가
        @JsonProperty("sbst_pric")         String substitutePrice  // 대용가격
    ) {}

    public boolean isSuccess() { return "0".equals(resultCode); }
}
