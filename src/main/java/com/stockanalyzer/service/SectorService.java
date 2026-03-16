package com.stockanalyzer.service;

import com.stockanalyzer.dto.StockDto;
import com.stockanalyzer.dto.response.SectorListResponse;
import com.stockanalyzer.dto.response.SectorStockResponse;
import com.stockanalyzer.mock.MockStockDataProvider;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SectorService {

    private final MockStockDataProvider mockDataProvider;

    public SectorService(MockStockDataProvider mockDataProvider) {
        this.mockDataProvider = mockDataProvider;
    }

    private static final Map<String, String> SECTOR_NAMES = Map.of(
        "semiconductor", "반도체",
        "it-platform",   "IT플랫폼",
        "fintech",       "핀테크",
        "game",          "게임",
        "bio",           "바이오",
        "automotive",    "자동차",
        "ai",            "AI"
    );

    public SectorListResponse getSectors() {
        Map<String, Long> countBySectorId = mockDataProvider.getAllStocks().stream()
                .collect(Collectors.groupingBy(MockStockDataProvider.MockStock::sectorId, Collectors.counting()));

        List<SectorListResponse.SectorDto> sectors = SECTOR_NAMES.entrySet().stream()
                .map(e -> new SectorListResponse.SectorDto(
                        e.getKey(),
                        e.getValue(),
                        countBySectorId.getOrDefault(e.getKey(), 0L).intValue()))
                .filter(s -> s.stockCount() > 0)
                .toList();

        return new SectorListResponse(sectors);
    }

    public SectorStockResponse getSectorStocks(String sectorId, String sort, String order) {
        Comparator<MockStockDataProvider.MockStock> comparator = switch (sort) {
            case "price"      -> Comparator.comparingLong(MockStockDataProvider.MockStock::price);
            case "changeRate" -> Comparator.comparingDouble(MockStockDataProvider.MockStock::changeRate);
            default           -> Comparator.comparingLong(MockStockDataProvider.MockStock::marketCap);
        };
        if ("desc".equals(order)) comparator = comparator.reversed();

        List<StockDto> stocks = mockDataProvider.getAllStocks().stream()
                .filter(s -> s.sectorId().equals(sectorId))
                .sorted(comparator)
                .map(s -> new StockDto(s.ticker(), s.name(), s.price(), s.changeRate(), s.marketCap(), s.sector()))
                .toList();

        return new SectorStockResponse(sectorId, SECTOR_NAMES.getOrDefault(sectorId, sectorId), stocks);
    }
}
