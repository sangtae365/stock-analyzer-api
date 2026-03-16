package com.stockanalyzer.service;

import com.stockanalyzer.dto.StockDto;
import com.stockanalyzer.dto.response.StockAnalysisResponse;
import com.stockanalyzer.dto.response.StockDetailResponse;
import com.stockanalyzer.dto.response.StockListResponse;
import com.stockanalyzer.mock.MockStockDataProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MockStockAnalysisService implements StockAnalysisService {

    private final MockStockDataProvider mockDataProvider;

    public MockStockAnalysisService(MockStockDataProvider mockDataProvider) {
        this.mockDataProvider = mockDataProvider;
    }

    @Override
    public StockListResponse getAllStocks(int page, int size, String sort, String order) {
        List<MockStockDataProvider.MockStock> all = mockDataProvider.getAllStocks();

        Comparator<MockStockDataProvider.MockStock> comparator = switch (sort) {
            case "price"      -> Comparator.comparingLong(MockStockDataProvider.MockStock::price);
            case "changeRate" -> Comparator.comparingDouble(MockStockDataProvider.MockStock::changeRate);
            default           -> Comparator.comparingLong(MockStockDataProvider.MockStock::marketCap);
        };
        if ("desc".equals(order)) comparator = comparator.reversed();

        List<StockDto> stocks = all.stream()
                .sorted(comparator)
                .skip((long) page * size)
                .limit(size)
                .map(this::toStockDto)
                .toList();

        return new StockListResponse(all.size(), page, size, stocks);
    }

    @Override
    public StockAnalysisResponse analyzeByKeyword(String keyword) {
        List<MockStockDataProvider.MockStock> matched = mockDataProvider.getAllStocks().stream()
                .filter(s -> s.relatedKeywords().stream()
                        .anyMatch(k -> k.contains(keyword) || keyword.contains(k)))
                .sorted(Comparator.comparingLong(MockStockDataProvider.MockStock::marketCap).reversed())
                .toList();

        int total       = matched.size();
        int leaderCount = (int) Math.ceil(total / 3.0);
        int growthCount = (int) Math.ceil((total - leaderCount) / 2.0);

        List<StockAnalysisResponse.AnalyzedStockDto> leaders   = matched.subList(0, leaderCount).stream().map(this::toAnalyzedDto).toList();
        List<StockAnalysisResponse.AnalyzedStockDto> growth    = matched.subList(leaderCount, leaderCount + growthCount).stream().map(this::toAnalyzedDto).toList();
        List<StockAnalysisResponse.AnalyzedStockDto> neglected = matched.subList(leaderCount + growthCount, total).stream().map(this::toAnalyzedDto).toList();

        List<StockAnalysisResponse.CategoryDto> categories = new ArrayList<>();
        if (!leaders.isEmpty())   categories.add(new StockAnalysisResponse.CategoryDto("대장주", leaders));
        if (!growth.isEmpty())    categories.add(new StockAnalysisResponse.CategoryDto("성장 기대주", growth));
        if (!neglected.isEmpty()) categories.add(new StockAnalysisResponse.CategoryDto("소외주", neglected));

        return new StockAnalysisResponse(keyword, LocalDateTime.now(), total, categories);
    }

    @Override
    public StockDetailResponse getStockDetail(String ticker) {
        return mockDataProvider.getAllStocks().stream()
                .filter(s -> s.ticker().equals(ticker))
                .findFirst()
                .map(s -> new StockDetailResponse(
                        s.ticker(), s.name(), s.price(), s.changeRate(),
                        s.marketCap(), s.sector(), s.aiAnalysis(), s.relatedKeywords()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다: " + ticker));
    }

    private StockDto toStockDto(MockStockDataProvider.MockStock s) {
        return new StockDto(s.ticker(), s.name(), s.price(), s.changeRate(), s.marketCap(), s.sector());
    }

    private StockAnalysisResponse.AnalyzedStockDto toAnalyzedDto(MockStockDataProvider.MockStock s) {
        return new StockAnalysisResponse.AnalyzedStockDto(s.ticker(), s.name(), s.price(), s.changeRate(), s.aiAnalysis());
    }
}
