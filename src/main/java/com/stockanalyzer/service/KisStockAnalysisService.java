package com.stockanalyzer.service;

import com.stockanalyzer.client.StockMarketClient;
import com.stockanalyzer.client.dto.KisChangeRateRankResponse;
import com.stockanalyzer.client.dto.KisChartResponse;
import com.stockanalyzer.client.dto.KisInvestorTrendResponse;
import com.stockanalyzer.client.dto.KisMarketCapRankResponse;
import com.stockanalyzer.client.dto.KisPriceResponse;
import com.stockanalyzer.client.dto.KisVolumeRankResponse;
import com.stockanalyzer.dto.StockDto;
import com.stockanalyzer.dto.response.InvestorTrendResponse;
import com.stockanalyzer.dto.response.StockAnalysisResponse;
import com.stockanalyzer.dto.response.StockChartResponse;
import com.stockanalyzer.dto.response.StockDetailResponse;
import com.stockanalyzer.dto.response.StockListResponse;
import com.stockanalyzer.dto.response.StockRankingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.stockanalyzer.service.MockStockPriceProvider;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Service
@Primary
public class KisStockAnalysisService implements StockAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(KisStockAnalysisService.class);

    private final StockMarketClient stockMarketClient;
    private final AiStockAnalysisService aiStockAnalysisService;
    private final MockStockPriceProvider mockPriceProvider;

    public KisStockAnalysisService(StockMarketClient stockMarketClient,
                                   AiStockAnalysisService aiStockAnalysisService,
                                   MockStockPriceProvider mockPriceProvider) {
        this.stockMarketClient = stockMarketClient;
        this.aiStockAnalysisService = aiStockAnalysisService;
        this.mockPriceProvider = mockPriceProvider;
    }

    // ── 전체 종목 조회 (시가총액 순) ──────────────────────────────────────

    @Override
    public StockListResponse getAllStocks(int page, int size, String sort, String order) {
        KisMarketCapRankResponse raw;
        try {
            raw = stockMarketClient.getMarketCapRanking("0")
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[종목목록] KIS 호출 실패 → 목업 폴백: {}", e.getMessage());
            return mockPriceProvider.getFallbackStockList();
        }
        if (raw == null) {
            log.warn("[종목목록] KIS 응답 없음 → 목업 폴백");
            return mockPriceProvider.getFallbackStockList();
        }

        List<StockDto> all = raw.items() == null ? List.of() :
                raw.items().stream()
                        .map(o -> new StockDto(
                                o.ticker(), o.name(),
                                parseLong(o.currentPrice()),
                                parseDouble(o.changeRate()),
                                parseLong(o.marketCap()) * 100_000_000L, // 억원 → 원
                                ""))
                        .toList();

        Comparator<StockDto> comparator = switch (sort) {
            case "price"      -> Comparator.comparingLong(StockDto::price);
            case "changeRate" -> Comparator.comparingDouble(StockDto::changeRate);
            default           -> Comparator.comparingLong(StockDto::marketCap).reversed();
        };
        if (!"marketCap".equals(sort) && "desc".equals(order)) comparator = comparator.reversed();

        List<StockDto> paged = all.stream()
                .sorted(comparator)
                .skip((long) page * size)
                .limit(size)
                .toList();

        return new StockListResponse(all.size(), page, size, paged);
    }

    // ── 키워드 분석 (AI 위임) ──────────────────────────────────────────────

    @Override
    public StockAnalysisResponse analyzeByKeyword(String keyword) {
        return aiStockAnalysisService.analyzeByKeyword(keyword);
    }

    // ── 종목 상세 ─────────────────────────────────────────────────────────

    @Override
    public StockDetailResponse getStockDetail(String ticker) {
        KisPriceResponse.Output out = stockMarketClient.getCurrentPriceRaw(ticker)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "KIS 현재가 조회 실패: " + ticker));
        return buildDetail(out, ticker);
    }

    private StockDetailResponse buildDetail(KisPriceResponse.Output o, String ticker) {
        String name   = (o.htsName() != null && !o.htsName().isBlank()) ? o.htsName() : ticker;
        String sector = o.sectorName() != null ? o.sectorName() : "";

        return new StockDetailResponse(
            ticker, name, sector, o.marketName(),
            parseLong(o.currentPrice()), parseLong(o.changeAmount()), parseDouble(o.changeRate()), o.changeSign(),
            parseLong(o.openPrice()), parseLong(o.highPrice()), parseLong(o.lowPrice()),
            parseLong(o.upperLimitPrice()), parseLong(o.lowerLimitPrice()),
            parseLong(o.accumulatedVolume()), parseLong(o.accumulatedAmount()),
            parseLong(o.marketCapBil()) * 100_000_000L,
            parseDouble(o.per()), parseDouble(o.pbr()), parseDouble(o.eps()), parseDouble(o.bps()),
            parseLong(o.listedShares()), parseLong(o.faceValue()), o.fiscalMonth(),
            parseDouble(o.volumeTurnover()),
            parseLong(o.w52HighPrice()), o.w52HighPriceDate(), parseDouble(o.w52HighPriceRate()),
            parseLong(o.w52LowPrice()),  o.w52LowPriceDate(),  parseDouble(o.w52LowPriceRate()),
            parseLong(o.yearHighPrice()), o.yearHighPriceDate(), parseDouble(o.yearHighPriceRate()),
            parseLong(o.yearLowPrice()),  o.yearLowPriceDate(),  parseDouble(o.yearLowPriceRate()),
            parseDouble(o.foreignExhaustionRate()), parseLong(o.foreignNetBuyQty()),
            parseDouble(o.loanRatio()),
            o.statusCode(), o.marketWarnCode(), o.viCode(), "Y".equals(o.shortSellYn()),
            parseDouble(o.pivotPoint()), parseDouble(o.pivot1Resist()), parseDouble(o.pivot1Support())
        );
    }

    // ── 차트 조회 ─────────────────────────────────────────────────────────

    @Override
    public StockChartResponse getStockChart(String ticker, String startDate, String endDate, String period) {
        KisChartResponse raw = stockMarketClient.getChartData(ticker, startDate, endDate, period)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "KIS 차트 조회 실패: " + ticker));

        KisChartResponse.Output1 s = raw.summary();
        StockChartResponse.Summary summary = new StockChartResponse.Summary(
            parseLong(s.currentPrice()),
            parseDouble(s.changeRate()),
            parseLong(s.changeAmount()),
            parseLong(s.volume()),
            parseLong(s.marketCapBil()),
            parseDouble(s.per()),
            parseDouble(s.pbr()),
            parseDouble(s.eps()),
            parseLong(s.listedShares()),
            parseLong(s.capital()),
            parseLong(s.faceValue()),
            parseDouble(s.volumeTurnover())
        );

        List<StockChartResponse.Candle> candles = raw.candles() == null ? List.of() :
            raw.candles().stream()
                .map(c -> new StockChartResponse.Candle(
                    c.date(),
                    parseLong(c.openPrice()),
                    parseLong(c.highPrice()),
                    parseLong(c.lowPrice()),
                    parseLong(c.closePrice()),
                    parseLong(c.volume()),
                    parseLong(c.amount()),
                    c.exDivCode()
                ))
                .toList();

        String name = s.name() != null ? s.name() : ticker;
        return new StockChartResponse(ticker, name, period, summary, candles);
    }

    // ── 투자자매매동향 ────────────────────────────────────────────────────

    @Override
    public InvestorTrendResponse getInvestorTrend(String ticker, String date) {
        KisInvestorTrendResponse raw = stockMarketClient.getInvestorTrend(ticker, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "KIS 투자자동향 조회 실패: " + ticker));

        KisInvestorTrendResponse.Output1 s = raw.summary();
        InvestorTrendResponse.Summary summary = new InvestorTrendResponse.Summary(
            parseLong(s.currentPrice()), parseDouble(s.changeRate()), parseLong(s.volume())
        );

        List<InvestorTrendResponse.DailyData> daily = raw.dailyData() == null ? List.of() :
            raw.dailyData().stream()
                .map(d -> new InvestorTrendResponse.DailyData(
                    d.date(), parseLong(d.closePrice()), parseLong(d.volume()), parseLong(d.amount()),
                    parseLong(d.foreignNetBuyQty()), parseLong(d.personNetBuyQty()),
                    parseLong(d.instNetBuyQty()), parseLong(d.securitiesNetBuyQty()),
                    parseLong(d.trustNetBuyQty()), parseLong(d.peFundNetBuyQty()),
                    parseLong(d.bankNetBuyQty()), parseLong(d.insuranceNetBuyQty()),
                    parseLong(d.pensionNetBuyQty()), parseLong(d.etcCorpNetBuyQty()),
                    parseLong(d.foreignNetBuyAmt()), parseLong(d.personNetBuyAmt()),
                    parseLong(d.instNetBuyAmt()), parseLong(d.securitiesNetBuyAmt()),
                    parseLong(d.trustNetBuyAmt()), parseLong(d.peFundNetBuyAmt()),
                    parseLong(d.bankNetBuyAmt()), parseLong(d.insuranceNetBuyAmt()),
                    parseLong(d.pensionNetBuyAmt()), parseLong(d.etcCorpNetBuyAmt())
                ))
                .toList();

        return new InvestorTrendResponse(ticker, date, summary, daily);
    }

    // ── 거래량 순위 ───────────────────────────────────────────────────────

    @Override
    public StockRankingResponse getVolumeRanking(String market) {
        KisVolumeRankResponse raw = stockMarketClient.getVolumeRanking(market)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "KIS 거래량순위 조회 실패"));

        List<StockRankingResponse.RankedStock> stocks = raw.items() == null ? List.of() :
            IntStream.range(0, raw.items().size())
                .mapToObj(i -> {
                    KisVolumeRankResponse.Output o = raw.items().get(i);
                    return new StockRankingResponse.RankedStock(
                        parseInt(o.rank()),
                        o.ticker(), o.name(),
                        parseLong(o.currentPrice()),
                        parseDouble(o.changeRate()),
                        parseLong(o.changeAmount()),
                        parseLong(o.volume()),
                        parseLong(o.totalAmount()),
                        parseDouble(o.volumeIncRate()),
                        0, 0
                    );
                })
                .toList();

        return new StockRankingResponse("volume", "거래량 순위", stocks);
    }

    // ── 등락률 순위 ───────────────────────────────────────────────────────

    @Override
    public StockRankingResponse getChangeRateRanking(String sort, String market) {
        KisChangeRateRankResponse raw = stockMarketClient.getChangeRateRanking(sort, market)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "KIS 등락률순위 조회 실패"));

        String sortLabel = "1".equals(sort) ? "하락률 순위" : "상승률 순위";

        List<StockRankingResponse.RankedStock> stocks = raw.items() == null ? List.of() :
            raw.items().stream()
                .map(o -> new StockRankingResponse.RankedStock(
                    parseInt(o.rank()),
                    o.ticker(), o.name(),
                    parseLong(o.currentPrice()),
                    parseDouble(o.changeRate()),
                    parseLong(o.changeAmount()),
                    parseLong(o.volume()),
                    0L,
                    0.0,
                    parseInt(o.consecutiveRisedays()),
                    parseInt(o.consecutiveFallDays())
                ))
                .toList();

        return new StockRankingResponse("changeRate", sortLabel, stocks);
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────

    private long parseLong(String v) {
        try { return v == null ? 0L : Long.parseLong(v.trim().replace(",", "")); }
        catch (NumberFormatException e) { return 0L; }
    }

    private double parseDouble(String v) {
        try { return v == null ? 0.0 : Double.parseDouble(v.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private int parseInt(String v) {
        try { return v == null ? 0 : Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
