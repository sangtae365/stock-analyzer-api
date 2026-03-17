package com.stockanalyzer.controller;

import com.stockanalyzer.dto.response.InvestorTrendResponse;
import com.stockanalyzer.dto.response.StockAnalysisResponse;
import com.stockanalyzer.dto.response.StockChartResponse;
import com.stockanalyzer.dto.response.StockDetailResponse;
import com.stockanalyzer.dto.response.StockListResponse;
import com.stockanalyzer.dto.response.StockRankingResponse;
import com.stockanalyzer.service.StockAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "종목", description = "주식 종목 조회 및 분석")
@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockAnalysisService stockAnalysisService;

    public StockController(StockAnalysisService stockAnalysisService) {
        this.stockAnalysisService = stockAnalysisService;
    }

    @Operation(summary = "전체 종목 목록 조회", description = "KIS 실시간 시세 기반 전체 종목을 페이징하여 반환합니다.")
    @GetMapping
    public ResponseEntity<StockListResponse> getAllStocks(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0")         int page,
            @Parameter(description = "페이지 크기")              @RequestParam(defaultValue = "20")        int size,
            @Parameter(description = "정렬 기준 (marketCap | price | changeRate)") @RequestParam(defaultValue = "marketCap") String sort,
            @Parameter(description = "정렬 방향 (desc | asc)") @RequestParam(defaultValue = "desc")      String order
    ) {
        return ResponseEntity.ok(stockAnalysisService.getAllStocks(page, size, sort, order));
    }

    @Operation(summary = "키워드 분석", description = "키워드로 관련 종목을 조회하고 대장주/성장기대주/소외주로 분류합니다.")
    @GetMapping("/analyze")
    public ResponseEntity<StockAnalysisResponse> analyzeByKeyword(
            @Parameter(description = "검색 키워드 (예: 카카오, AI, 반도체)") @RequestParam String keyword
    ) {
        return ResponseEntity.ok(stockAnalysisService.analyzeByKeyword(keyword));
    }

    @Operation(summary = "종목 상세 조회", description = "종목 코드로 실시간 시세와 상세 정보를 조회합니다. (52주 고저가, 외국인소진율, 피벗포인트 등 포함)")
    @GetMapping("/{ticker}")
    public ResponseEntity<StockDetailResponse> getStockDetail(
            @Parameter(description = "종목 코드 (예: 005930)") @PathVariable String ticker
    ) {
        return ResponseEntity.ok(stockAnalysisService.getStockDetail(ticker));
    }

    @Operation(summary = "기간별 차트 조회", description = "일/주/월/년봉 OHLCV 차트 데이터를 조회합니다. (KIS FHKST03010100, 최대 100건)")
    @GetMapping("/{ticker}/chart")
    public ResponseEntity<StockChartResponse> getStockChart(
            @Parameter(description = "종목 코드 (예: 005930)") @PathVariable String ticker,
            @Parameter(description = "시작일 (YYYYMMDD)") @RequestParam String startDate,
            @Parameter(description = "종료일 (YYYYMMDD)") @RequestParam String endDate,
            @Parameter(description = "기간 구분 (D:일봉 W:주봉 M:월봉 Y:년봉)") @RequestParam(defaultValue = "D") String period
    ) {
        return ResponseEntity.ok(stockAnalysisService.getStockChart(ticker, startDate, endDate, period));
    }

    @Operation(summary = "투자자별 매매동향 조회", description = "외국인/기관/개인 등 투자자별 일별 순매수 데이터를 조회합니다. (KIS FHPTJ04160001, 실전 전용, 15:40 이후 조회 가능)")
    @GetMapping("/{ticker}/investor-trend")
    public ResponseEntity<InvestorTrendResponse> getInvestorTrend(
            @Parameter(description = "종목 코드 (예: 005930)") @PathVariable String ticker,
            @Parameter(description = "조회 기준일 (YYYYMMDD, 기본값: 오늘)") @RequestParam String date
    ) {
        return ResponseEntity.ok(stockAnalysisService.getInvestorTrend(ticker, date));
    }

    @Operation(summary = "거래량 순위", description = "거래량 상위 종목 최대 30건을 조회합니다. (KIS FHPST01710000, 실전 전용)")
    @GetMapping("/ranking/volume")
    public ResponseEntity<StockRankingResponse> getVolumeRanking(
            @Parameter(description = "시장 코드 (0000:전체, 0001:코스피, 1001:코스닥)") @RequestParam(defaultValue = "0000") String market
    ) {
        return ResponseEntity.ok(stockAnalysisService.getVolumeRanking(market));
    }

    @Operation(summary = "등락률 순위", description = "등락률 상위/하위 종목 최대 30건을 조회합니다. (KIS FHPST01700000, 실전 전용)")
    @GetMapping("/ranking/change-rate")
    public ResponseEntity<StockRankingResponse> getChangeRateRanking(
            @Parameter(description = "정렬 (0:상승률 순위, 1:하락률 순위)") @RequestParam(defaultValue = "0") String sort,
            @Parameter(description = "시장 코드 (0000:전체, 0001:코스피, 1001:코스닥, 2001:코스피200)") @RequestParam(defaultValue = "0000") String market
    ) {
        return ResponseEntity.ok(stockAnalysisService.getChangeRateRanking(sort, market));
    }
}
