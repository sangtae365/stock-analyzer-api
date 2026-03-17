package com.stockanalyzer.service;

import com.stockanalyzer.dto.response.InvestorTrendResponse;
import com.stockanalyzer.dto.response.StockAnalysisResponse;
import com.stockanalyzer.dto.response.StockChartResponse;
import com.stockanalyzer.dto.response.StockDetailResponse;
import com.stockanalyzer.dto.response.StockListResponse;
import com.stockanalyzer.dto.response.StockRankingResponse;

public interface StockAnalysisService {
    StockListResponse getAllStocks(int page, int size, String sort, String order);
    StockAnalysisResponse analyzeByKeyword(String keyword);
    StockDetailResponse getStockDetail(String ticker);
    StockChartResponse getStockChart(String ticker, String startDate, String endDate, String period);
    InvestorTrendResponse getInvestorTrend(String ticker, String date);
    StockRankingResponse getVolumeRanking(String market);
    StockRankingResponse getChangeRateRanking(String sort, String market);
}
