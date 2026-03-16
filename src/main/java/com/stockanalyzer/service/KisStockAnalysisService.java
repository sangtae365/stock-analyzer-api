package com.stockanalyzer.service;

import com.stockanalyzer.client.StockMarketClient;
import com.stockanalyzer.dto.StockDto;
import com.stockanalyzer.dto.response.StockAnalysisResponse;
import com.stockanalyzer.dto.response.StockDetailResponse;
import com.stockanalyzer.dto.response.StockListResponse;
import com.stockanalyzer.mock.MockStockDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * KIS OpenAPI 기반 실제 시세 서비스.
 * API 키 미설정 또는 KIS 장애 시 Mock 데이터로 자동 폴백.
 * @Primary 로 MockStockAnalysisService 를 대체.
 */
@Service
@Primary
public class KisStockAnalysisService implements StockAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(KisStockAnalysisService.class);

    private final StockMarketClient stockMarketClient;
    private final MockStockDataProvider mockDataProvider;

    public KisStockAnalysisService(StockMarketClient stockMarketClient,
                                   MockStockDataProvider mockDataProvider) {
        this.stockMarketClient = stockMarketClient;
        this.mockDataProvider  = mockDataProvider;
    }

    // ── 전체 종목 조회 ────────────────────────────────────────────────────

    @Override
    public StockListResponse getAllStocks(int page, int size, String sort, String order) {
        List<StockDto> all = mockDataProvider.getAllStocks().stream()
                .map(meta -> stockMarketClient.getCurrentPrice(meta.ticker(), meta.sector())
                        .orElseGet(() -> fallbackStockDto(meta)))
                .toList();

        Comparator<StockDto> comparator = switch (sort) {
            case "price"      -> Comparator.comparingLong(StockDto::price);
            case "changeRate" -> Comparator.comparingDouble(StockDto::changeRate);
            default           -> Comparator.comparingLong(StockDto::marketCap);
        };
        if ("desc".equals(order)) comparator = comparator.reversed();

        List<StockDto> paged = all.stream()
                .sorted(comparator)
                .skip((long) page * size)
                .limit(size)
                .toList();

        return new StockListResponse(all.size(), page, size, paged);
    }

    // ── 키워드 분석 ───────────────────────────────────────────────────────

    @Override
    public StockAnalysisResponse analyzeByKeyword(String keyword) {
        List<MockStockDataProvider.MockStock> matched = mockDataProvider.getAllStocks().stream()
                .filter(s -> s.relatedKeywords().stream()
                        .anyMatch(k -> k.contains(keyword) || keyword.contains(k)))
                .toList();

        // 실시간 시세로 보강, 실패 시 Mock 폴백
        List<StockDto> enriched = matched.stream()
                .map(meta -> stockMarketClient.getCurrentPrice(meta.ticker(), meta.sector())
                        .orElseGet(() -> fallbackStockDto(meta)))
                .sorted(Comparator.comparingLong(StockDto::marketCap).reversed())
                .toList();

        int total       = enriched.size();
        int leaderCount = (int) Math.ceil(total / 3.0);
        int growthCount = (int) Math.ceil((total - leaderCount) / 2.0);

        List<StockAnalysisResponse.AnalyzedStockDto> leaders   = toAnalyzedList(enriched, matched, 0, leaderCount);
        List<StockAnalysisResponse.AnalyzedStockDto> growth    = toAnalyzedList(enriched, matched, leaderCount, leaderCount + growthCount);
        List<StockAnalysisResponse.AnalyzedStockDto> neglected = toAnalyzedList(enriched, matched, leaderCount + growthCount, total);

        List<StockAnalysisResponse.CategoryDto> categories = new ArrayList<>();
        if (!leaders.isEmpty())   categories.add(new StockAnalysisResponse.CategoryDto("대장주",     leaders));
        if (!growth.isEmpty())    categories.add(new StockAnalysisResponse.CategoryDto("성장 기대주", growth));
        if (!neglected.isEmpty()) categories.add(new StockAnalysisResponse.CategoryDto("소외주",     neglected));

        return new StockAnalysisResponse(keyword, LocalDateTime.now(), total, categories);
    }

    // ── 종목 상세 ─────────────────────────────────────────────────────────

    @Override
    public StockDetailResponse getStockDetail(String ticker) {
        MockStockDataProvider.MockStock meta = mockDataProvider.getAllStocks().stream()
                .filter(s -> s.ticker().equals(ticker))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다: " + ticker));

        StockDto price = stockMarketClient.getCurrentPrice(meta.ticker(), meta.sector())
                .orElseGet(() -> fallbackStockDto(meta));

        return new StockDetailResponse(
                price.ticker(), price.name(), price.price(), price.changeRate(),
                price.marketCap(), price.sector(), meta.aiAnalysis(), meta.relatedKeywords());
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────

    /** KIS 호출 실패 시 Mock 데이터로 폴백 */
    private StockDto fallbackStockDto(MockStockDataProvider.MockStock meta) {
        log.warn("KIS 폴백 적용: {}", meta.ticker());
        return new StockDto(meta.ticker(), meta.name(), meta.price(),
                meta.changeRate(), meta.marketCap(), meta.sector());
    }

    private List<StockAnalysisResponse.AnalyzedStockDto> toAnalyzedList(
            List<StockDto> enriched,
            List<MockStockDataProvider.MockStock> metas,
            int from, int to) {

        return enriched.subList(from, to).stream()
                .map(s -> {
                    String summary = metas.stream()
                            .filter(m -> m.ticker().equals(s.ticker()))
                            .findFirst()
                            .map(MockStockDataProvider.MockStock::aiAnalysis)
                            .orElse("");
                    return new StockAnalysisResponse.AnalyzedStockDto(
                            s.ticker(), s.name(), s.price(), s.changeRate(), summary);
                })
                .toList();
    }
}
