package com.stockanalyzer.controller;

import com.stockanalyzer.dto.response.StockAnalysisResponse;
import com.stockanalyzer.dto.response.StockDetailResponse;
import com.stockanalyzer.dto.response.StockListResponse;
import com.stockanalyzer.service.StockAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockAnalysisService stockAnalysisService;

    public StockController(StockAnalysisService stockAnalysisService) {
        this.stockAnalysisService = stockAnalysisService;
    }

    @GetMapping
    public ResponseEntity<StockListResponse> getAllStocks(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "20")        int size,
            @RequestParam(defaultValue = "marketCap") String sort,
            @RequestParam(defaultValue = "desc")      String order
    ) {
        return ResponseEntity.ok(stockAnalysisService.getAllStocks(page, size, sort, order));
    }

    @GetMapping("/analyze")
    public ResponseEntity<StockAnalysisResponse> analyzeByKeyword(@RequestParam String keyword) {
        return ResponseEntity.ok(stockAnalysisService.analyzeByKeyword(keyword));
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<StockDetailResponse> getStockDetail(@PathVariable String ticker) {
        return ResponseEntity.ok(stockAnalysisService.getStockDetail(ticker));
    }
}
