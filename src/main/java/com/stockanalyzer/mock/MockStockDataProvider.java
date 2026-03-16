package com.stockanalyzer.mock;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockStockDataProvider {

    public record MockStock(
        String ticker,
        String name,
        long price,
        double changeRate,
        long marketCap,
        String sector,
        String sectorId,
        String aiAnalysis,
        List<String> relatedKeywords
    ) {}

    public List<MockStock> getAllStocks() {
        return List.of(
            new MockStock("005930", "삼성전자",      72000,  1.23, 429_000_000_000_000L, "반도체",   "semiconductor", "글로벌 메모리 반도체 1위, HBM 기술 선도",                 List.of("반도체", "AI", "HBM")),
            new MockStock("000660", "SK하이닉스",   178000,  2.15, 129_000_000_000_000L, "반도체",   "semiconductor", "HBM3E 엔비디아 납품 확대로 실적 급성장",                  List.of("반도체", "AI", "HBM")),
            new MockStock("035420", "NAVER",        175000,  0.86,  28_700_000_000_000L, "IT플랫폼", "it-platform",   "하이퍼클로바X 기반 AI 서비스 전방위 확장",                List.of("IT플랫폼", "AI")),
            new MockStock("035720", "카카오",         45000, -0.44,  19_000_000_000_000L, "IT플랫폼", "it-platform",   "카카오 생태계 플랫폼 중심, AI 서비스 확장 중",            List.of("카카오", "IT플랫폼", "AI")),
            new MockStock("377300", "카카오페이",     24500,  3.12,  29_400_000_000_000L, "핀테크",   "fintech",       "AI 기반 리스크 분석 고도화로 B2B 확장 중",                List.of("카카오", "핀테크", "AI", "금융")),
            new MockStock("293490", "카카오게임즈",   15800,  1.54,   5_200_000_000_000L, "게임",     "game",          "AI 기반 게임 추천 시스템 도입으로 ARPU 성장 기대",        List.of("카카오", "게임")),
            new MockStock("323410", "카카오뱅크",    19200, -0.52,   9_100_000_000_000L, "핀테크",   "fintech",       "수익성 우려로 관망세이나 AI 여신심사 고도화 진행 중",     List.of("카카오", "핀테크", "금융")),
            new MockStock("404950", "IMA",            8200,  5.78,   1_200_000_000_000L, "AI",       "ai",            "카카오 생태계 연동 AI 마케팅 솔루션 확장 중",             List.of("AI", "카카오")),
            new MockStock("207940", "삼성바이오로직스", 885000, 0.34, 63_000_000_000_000L, "바이오",  "bio",           "글로벌 CMO 1위, AI 신약개발 플랫폼 도입",                 List.of("바이오", "AI")),
            new MockStock("068270", "셀트리온",      155000, -1.02,  19_800_000_000_000L, "바이오",   "bio",           "바이오시밀러 글로벌 시장 점유율 확대 중",                 List.of("바이오")),
            new MockStock("259960", "크래프톤",      285000,  1.23,  12_600_000_000_000L, "게임",     "game",          "글로벌 배틀그라운드 IP 기반 신작 흥행 기대",              List.of("게임")),
            new MockStock("005380", "현대차",        245000,  0.41,  52_300_000_000_000L, "자동차",   "automotive",    "전기차 및 자율주행 AI 기술 투자 확대",                    List.of("자동차", "AI", "전기차"))
        );
    }
}
