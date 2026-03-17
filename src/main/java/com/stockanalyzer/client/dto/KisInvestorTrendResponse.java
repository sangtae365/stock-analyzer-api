package com.stockanalyzer.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * KIS FHPTJ04160001 - 종목별 투자자매매동향(일별) 응답
 * GET /uapi/domestic-stock/v1/quotations/investor-trend-estimate
 * 실전만 지원 (모의 미지원), 당일 데이터는 15:40 이후 조회 가능
 * 금액 단위: 백만원, 수량 단위: 주
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisInvestorTrendResponse(
    @JsonProperty("rt_cd")   String resultCode,
    @JsonProperty("msg_cd")  String messageCode,
    @JsonProperty("msg1")    String message,
    @JsonProperty("output1") Output1 summary,
    @JsonProperty("output2") List<Output2> dailyData
) {
    /** 현재가 요약 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output1(
        @JsonProperty("stck_prpr")          String currentPrice,
        @JsonProperty("prdy_vrss")          String changeAmount,
        @JsonProperty("prdy_vrss_sign")     String changeSign,
        @JsonProperty("prdy_ctrt")          String changeRate,
        @JsonProperty("acml_vol")           String volume,
        @JsonProperty("prdy_vol")           String prevVolume,
        @JsonProperty("rprs_mrkt_kor_name") String marketName
    ) {}

    /** 일별 투자자별 매매 데이터 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output2(
        // 기본 시세
        @JsonProperty("stck_bsop_date") String date,         // 영업 일자 (YYYYMMDD)
        @JsonProperty("stck_clpr")      String closePrice,   // 종가
        @JsonProperty("stck_oprc")      String openPrice,    // 시가
        @JsonProperty("stck_hgpr")      String highPrice,    // 최고가
        @JsonProperty("stck_lwpr")      String lowPrice,     // 최저가
        @JsonProperty("acml_vol")       String volume,       // 거래량 (주)
        @JsonProperty("acml_tr_pbmn")   String amount,       // 거래대금 (백만원)

        // 순매수 수량
        @JsonProperty("frgn_ntby_qty")  String foreignNetBuyQty,  // 외국인 순매수
        @JsonProperty("prsn_ntby_qty")  String personNetBuyQty,   // 개인 순매수
        @JsonProperty("orgn_ntby_qty")  String instNetBuyQty,     // 기관계 순매수
        @JsonProperty("scrt_ntby_qty")  String securitiesNetBuyQty, // 증권 순매수
        @JsonProperty("ivtr_ntby_qty")  String trustNetBuyQty,    // 투자신탁 순매수
        @JsonProperty("pe_fund_ntby_vol") String peFundNetBuyQty, // 사모펀드 순매수
        @JsonProperty("bank_ntby_qty")  String bankNetBuyQty,     // 은행 순매수
        @JsonProperty("insu_ntby_qty")  String insuranceNetBuyQty, // 보험 순매수
        @JsonProperty("fund_ntby_qty")  String pensionNetBuyQty,  // 기금 순매수
        @JsonProperty("etc_corp_ntby_vol") String etcCorpNetBuyQty, // 기타법인 순매수

        // 순매수 대금 (백만원)
        @JsonProperty("frgn_ntby_tr_pbmn")     String foreignNetBuyAmt,
        @JsonProperty("prsn_ntby_tr_pbmn")     String personNetBuyAmt,
        @JsonProperty("orgn_ntby_tr_pbmn")     String instNetBuyAmt,
        @JsonProperty("scrt_ntby_tr_pbmn")     String securitiesNetBuyAmt,
        @JsonProperty("ivtr_ntby_tr_pbmn")     String trustNetBuyAmt,
        @JsonProperty("pe_fund_ntby_tr_pbmn")  String peFundNetBuyAmt,
        @JsonProperty("bank_ntby_tr_pbmn")     String bankNetBuyAmt,
        @JsonProperty("insu_ntby_tr_pbmn")     String insuranceNetBuyAmt,
        @JsonProperty("fund_ntby_tr_pbmn")     String pensionNetBuyAmt,
        @JsonProperty("etc_corp_ntby_tr_pbmn") String etcCorpNetBuyAmt
    ) {}

    public boolean isSuccess() { return "0".equals(resultCode); }
}
