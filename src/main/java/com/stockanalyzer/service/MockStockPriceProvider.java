package com.stockanalyzer.service;

import com.stockanalyzer.dto.StockDto;
import com.stockanalyzer.dto.response.StockAnalysisResponse;
import com.stockanalyzer.dto.response.StockListResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * KIS / Gemini API 연결 불가 시 사용하는 목업 데이터.
 */
@Component
public class MockStockPriceProvider {

    public record MockPrice(long price, double changeRate) {}

    // ── 시세 목업 ────────────────────────────────────────────────────────────

    private static final Map<String, MockPrice> PRICE_DATA = Map.ofEntries(
        // 반도체 / AI 인프라
        Map.entry("005930", new MockPrice(75000,   1.2)),   // 삼성전자
        Map.entry("000660", new MockPrice(180000,  2.3)),   // SK하이닉스
        Map.entry("042700", new MockPrice(95000,  -0.5)),   // 한미반도체
        Map.entry("058470", new MockPrice(112000,  0.9)),   // 리노공업
        Map.entry("095340", new MockPrice(52000,  -1.1)),   // ISC
        Map.entry("319660", new MockPrice(38000,   0.3)),   // 피에스케이
        Map.entry("007660", new MockPrice(28000,   1.8)),   // 이수페타시스
        Map.entry("080580", new MockPrice(19000,  -0.5)),   // 오킨스전자
        Map.entry("131290", new MockPrice(115000,  0.4)),   // 티에스이
        Map.entry("131970", new MockPrice(73000,   2.1)),   // 두산테스나
        Map.entry("147760", new MockPrice(4000,   -1.2)),   // 마이크로프랜드

        // AI 소프트웨어 / 플랫폼
        Map.entry("035420", new MockPrice(195000,  0.5)),   // NAVER
        Map.entry("035720", new MockPrice(42000,  -0.7)),   // 카카오
        Map.entry("017670", new MockPrice(52000,   1.0)),   // SK텔레콤
        Map.entry("030200", new MockPrice(38000,   0.8)),   // KT
        Map.entry("064520", new MockPrice(78000,   1.5)),   // LG CNS
        Map.entry("304100", new MockPrice(25000,  -2.0)),   // 솔트룩스
        Map.entry("402030", new MockPrice(15000,   1.2)),   // 코난테크놀로지
        Map.entry("338220", new MockPrice(18000,   3.1)),   // 뷰노
        Map.entry("328130", new MockPrice(62000,   2.5)),   // 루닛
        Map.entry("101060", new MockPrice(8500,   -1.5)),   // IMA

        // 카카오 그룹
        Map.entry("377300", new MockPrice(22000,  -0.9)),   // 카카오페이
        Map.entry("293490", new MockPrice(14000,  -1.4)),   // 카카오게임즈
        Map.entry("323410", new MockPrice(24000,   0.4)),   // 카카오뱅크

        // 바이오 / 헬스케어
        Map.entry("207940", new MockPrice(820000,  0.6)),   // 삼성바이오로직스
        Map.entry("068270", new MockPrice(175000,  1.2)),   // 셀트리온
        Map.entry("000100", new MockPrice(75000,   0.3)),   // 유한양행
        Map.entry("128940", new MockPrice(340000, -0.6)),   // 한미약품

        // 2차전지 / 친환경
        Map.entry("373220", new MockPrice(350000, -1.4)),   // LG에너지솔루션
        Map.entry("006400", new MockPrice(280000, -0.7)),   // 삼성SDI
        Map.entry("096770", new MockPrice(115000,  0.4)),   // SK이노베이션
        Map.entry("086520", new MockPrice(55000,  -2.1)),   // 에코프로
        Map.entry("247540", new MockPrice(120000, -1.8)),   // 에코프로비엠
        Map.entry("003670", new MockPrice(210000, -0.5)),   // 포스코퓨처엠
        Map.entry("066970", new MockPrice(95000,  -1.2)),   // 엘앤에프

        // 자동차 / 모빌리티
        Map.entry("005380", new MockPrice(215000,  0.9)),   // 현대차
        Map.entry("000270", new MockPrice(95000,   1.1)),   // 기아
        Map.entry("012330", new MockPrice(245000,  0.4)),   // 현대모비스
        Map.entry("064350", new MockPrice(48000,   2.3)),   // 현대로템
        Map.entry("066570", new MockPrice(88000,   0.7)),   // LG전자

        // 방산 / 로봇
        Map.entry("012450", new MockPrice(620000,  1.5)),   // 한화에어로스페이스
        Map.entry("047810", new MockPrice(72000,   0.8)),   // 한국항공우주
        Map.entry("277810", new MockPrice(145000,  3.2)),   // 레인보우로보틱스
        Map.entry("267250", new MockPrice(85000,   1.0)),   // HD현대

        // 금융
        Map.entry("105560", new MockPrice(88000,   0.5)),   // KB금융
        Map.entry("055550", new MockPrice(52000,   0.3)),   // 신한지주
        Map.entry("086790", new MockPrice(65000,   0.7)),   // 하나금융지주

        // 게임
        Map.entry("259960", new MockPrice(235000,  1.3)),   // 크래프톤
        Map.entry("251270", new MockPrice(48000,  -0.8)),   // 넷마블
        Map.entry("036570", new MockPrice(175000, -1.5)),   // 엔씨소프트
        Map.entry("078340", new MockPrice(28000,  -0.4))    // 컴투스
    );

    // ── 카테고리 목업 (Gemini 실패 시 폴백) ─────────────────────────────────

    private static final List<StockAnalysisResponse.CategoryDto> FALLBACK_CATEGORIES = List.of(
        new StockAnalysisResponse.CategoryDto("대장주", List.of(
            stock("005930", "삼성전자",      75000,   1.2, "[매수] 반도체·AI 시대 핵심 대장주 (임시 데이터)"),
            stock("000660", "SK하이닉스",   180000,   2.3, "[매수] HBM 글로벌 1위, AI 반도체 핵심 공급사 (임시 데이터)"),
            stock("035420", "NAVER",        195000,   0.5, "[매수] AI·플랫폼 대표 종목 (임시 데이터)")
        )),
        new StockAnalysisResponse.CategoryDto("성장 기대주", List.of(
            stock("042700", "한미반도체",    95000,  -0.5, "[매수] HBM 장비 핵심 공급사, 성장 잠재력 보유 (임시 데이터)"),
            stock("328130", "루닛",          62000,   2.5, "[매수] AI 의료영상 분석 글로벌 확장 중 (임시 데이터)"),
            stock("277810", "레인보우로보틱스", 145000, 3.2, "[매수] 로봇 AI 결합 성장 기대주 (임시 데이터)")
        )),
        new StockAnalysisResponse.CategoryDto("소외주", List.of(
            stock("304100", "솔트룩스",      25000,  -2.0, "[중립] 국내 LLM 개발사, 시장 관심 낮음 (임시 데이터)"),
            stock("101060", "IMA",            8500,  -1.5, "[관망] AI 금융 솔루션 잠재력 보유 (임시 데이터)"),
            stock("402030", "코난테크놀로지", 15000,   1.2, "[중립] AI 검색·분석 전문, 저평가 구간 (임시 데이터)")
        ))
    );

    private static StockAnalysisResponse.AnalyzedStockDto stock(
            String ticker, String name, long price, double changeRate, String summary) {
        return new StockAnalysisResponse.AnalyzedStockDto(ticker, name, price, changeRate, summary);
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /** 종목코드로 목업 시세 조회 */
    public Optional<MockPrice> get(String ticker) {
        MockPrice mock = PRICE_DATA.getOrDefault(ticker, new MockPrice(0L, 0.0));
        return Optional.of(mock);
    }

    /** KIS 실패 시 시가총액 상위 목업 종목 리스트 반환 */
    public StockListResponse getFallbackStockList() {
        List<StockDto> stocks = List.of(
            new StockDto("005930", "삼성전자",      75000,   1.2, 4_000_000_000_000_000L, "반도체"),
            new StockDto("000660", "SK하이닉스",   180000,   2.3,  130_000_000_000_000L, "반도체"),
            new StockDto("207940", "삼성바이오로직스", 820000, 0.6,  110_000_000_000_000L, "바이오"),
            new StockDto("005380", "현대차",       215000,   0.9,   90_000_000_000_000L, "자동차"),
            new StockDto("006400", "삼성SDI",      280000,  -0.7,   65_000_000_000_000L, "2차전지"),
            new StockDto("035420", "NAVER",        195000,   0.5,   32_000_000_000_000L, "IT"),
            new StockDto("000270", "기아",          95000,   1.1,   38_000_000_000_000L, "자동차"),
            new StockDto("068270", "셀트리온",     175000,   1.2,   24_000_000_000_000L, "바이오"),
            new StockDto("373220", "LG에너지솔루션", 350000, -1.4,   82_000_000_000_000L, "2차전지"),
            new StockDto("012450", "한화에어로스페이스", 620000, 1.5, 25_000_000_000_000L, "방산"),
            new StockDto("035720", "카카오",        42000,  -0.7,   18_000_000_000_000L, "IT"),
            new StockDto("105560", "KB금융",        88000,   0.5,   36_000_000_000_000L, "금융"),
            new StockDto("055550", "신한지주",       52000,   0.3,   26_000_000_000_000L, "금융"),
            new StockDto("086790", "하나금융지주",   65000,   0.7,   19_000_000_000_000L, "금융"),
            new StockDto("003670", "포스코퓨처엠",  210000,  -0.5,   18_000_000_000_000L, "2차전지"),
            new StockDto("259960", "크래프톤",      235000,   1.3,   19_000_000_000_000L, "게임"),
            new StockDto("096770", "SK이노베이션",  115000,   0.4,   12_000_000_000_000L, "에너지"),
            new StockDto("042700", "한미반도체",     95000,  -0.5,   10_000_000_000_000L, "반도체"),
            new StockDto("128940", "한미약품",      340000,  -0.6,   11_000_000_000_000L, "바이오"),
            new StockDto("012330", "현대모비스",    245000,   0.4,   23_000_000_000_000L, "자동차"),
            new StockDto("066570", "LG전자",        88000,   0.7,   14_000_000_000_000L, "전자"),
            new StockDto("323410", "카카오뱅크",     24000,   0.4,   11_000_000_000_000L, "금융"),
            new StockDto("064520", "LG CNS",        78000,   1.5,    9_000_000_000_000L, "IT"),
            new StockDto("028260", "삼성물산",      155000,   0.6,   29_000_000_000_000L, "건설"),
            new StockDto("017670", "SK텔레콤",       52000,   1.0,   13_000_000_000_000L, "통신"),
            new StockDto("030200", "KT",             38000,   0.8,    9_000_000_000_000L, "통신"),
            new StockDto("058470", "리노공업",      112000,   0.9,    2_000_000_000_000L, "반도체"),
            new StockDto("277810", "레인보우로보틱스", 145000, 3.2,   2_500_000_000_000L, "로봇"),
            new StockDto("328130", "루닛",           62000,   2.5,    1_500_000_000_000L, "바이오"),
            new StockDto("086520", "에코프로",       55000,  -2.1,    3_000_000_000_000L, "2차전지")
        );
        return new StockListResponse(stocks.size(), 0, stocks.size(), stocks);
    }

    /** Gemini 실패 시 전체 목업 분석 결과 반환 */
    public StockAnalysisResponse getFallbackAnalysis(String keyword) {
        return new StockAnalysisResponse(
            keyword + " (AI 서비스 일시 중단 - 임시 데이터)",
            LocalDateTime.now(),
            9,
            FALLBACK_CATEGORIES
        );
    }
}
