package com.stockanalyzer.service;

import com.stockanalyzer.dto.response.StockAnalysisResponse;
import com.stockanalyzer.dto.response.StockDetailResponse;
import com.stockanalyzer.dto.response.StockListResponse;

public interface StockAnalysisService {
    StockListResponse getAllStocks(int page, int size, String sort, String order);
    StockAnalysisResponse analyzeByKeyword(String keyword);
    StockDetailResponse getStockDetail(String ticker);
}
