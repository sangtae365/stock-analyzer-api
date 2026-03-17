package com.stockanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import com.stockanalyzer.client.StockMarketClient;
import com.stockanalyzer.client.dto.KisPriceResponse;
import com.stockanalyzer.dto.response.StockAnalysisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AiStockAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiStockAnalysisService.class);

    private final WebClient geminiClient;
    private final StockMarketClient stockMarketClient;
    private final MockStockPriceProvider mockPriceProvider;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;

    // ┌─────────────────────────────────────────────────────────────────────────┐
    // │                         S Y S T E M   P R O M P T                       │
    // └─────────────────────────────────────────────────────────────────────────┘
    private static final String SYSTEM_PROMPT = """
            당신은 한국 주식 시장 전문 애널리스트입니다.
            사용자가 투자 테마 키워드 또는 특정 종목명을 입력하면,
            관련 한국 상장 종목을 '대장주', '성장 기대주', '소외주' 3가지 카테고리로 분류해 분석합니다.

            [종목 선정 원칙]
            - 코스피·코스닥에 상장된 모든 종목을 대상으로 자유롭게 선정하세요.
            - 특정 목록에 제한받지 말고, 키워드와 관련된 종목이라면 어떤 종목이든 포함하세요.
            - 키워드가 특정 종목명인 경우, 해당 종목을 반드시 포함하고 관련 종목들도 함께 선정하세요.
            - 시가총액, 유동성, 업종 연관성, 성장성 등을 종합적으로 고려하세요.

            [카테고리 분류 기준]
            - 대장주: 해당 테마 내 시가총액 상위, 높은 유동성, 테마를 대표하는 핵심 종목
            - 성장 기대주: 성장 잠재력이 높고 향후 주가 상승이 기대되나 아직 덜 알려진 종목
            - 소외주: 테마 관련성은 있으나 현재 시장에서 상대적으로 주목받지 못하는 종목

            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            [출력 규칙 - 이것만 반드시 지키세요]
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            1. 오직 아래 JSON 형식만 출력하세요.
               ```json 같은 마크다운 코드블록, 설명 문장, 줄바꿈 접두사 절대 금지.
               첫 글자는 반드시 { 이어야 합니다.
            2. 각 카테고리당 정확히 2~3개 종목만 선정하세요.
            3. ticker는 반드시 실제 코스피/코스닥에 상장된 6자리 숫자 종목코드여야 합니다.
               존재하지 않는 코드를 절대 만들어내지 마세요.
            4. reason은 50자 이내 한국어로 작성하세요.
            5. recommendation은 "적극매수", "매수", "중립", "관망" 중 하나만 사용하세요.

            [출력 JSON 스키마]
            {"categories":[{"category":"대장주","stocks":[{"name":"종목명","ticker":"000000","reason":"한 줄 이유","recommendation":"매수"}]},{"category":"성장 기대주","stocks":[{"name":"종목명","ticker":"000000","reason":"한 줄 이유","recommendation":"매수"}]},{"category":"소외주","stocks":[{"name":"종목명","ticker":"000000","reason":"한 줄 이유","recommendation":"중립"}]}]}
            """;
    // ┌─────────────────────────────────────────────────────────────────────────┐
    // │                     E N D   O F   P R O M P T                           │
    // └─────────────────────────────────────────────────────────────────────────┘

    public AiStockAnalysisService(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model,
            StockMarketClient stockMarketClient,
            MockStockPriceProvider mockPriceProvider) {
        this.apiKey = apiKey;
        this.model = model;
        this.stockMarketClient = stockMarketClient;
        this.mockPriceProvider = mockPriceProvider;
        this.objectMapper = new ObjectMapper();
        this.geminiClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    // ── ① 메인 오케스트레이션 ────────────────────────────────────────────────

    public StockAnalysisResponse analyzeByKeyword(String keyword) {

        // Step 1. LLM 호출 (실패 시 목업 폴백)
        String rawJson;
        try {
            rawJson = callGemini(keyword);
            log.info("[AI분석] keyword='{}' | 응답길이={}", keyword, rawJson.length());
        } catch (Exception e) {
            log.warn("[AI분석] Gemini 호출 실패 → 목업 폴백: {}", e.getMessage());
            return mockPriceProvider.getFallbackAnalysis(keyword);
        }

        // Step 2. JSON 파싱
        JsonNode categories = parseLlmResponse(rawJson);
        if (categories == null || !categories.isArray()) {
            log.warn("[AI분석] JSON 파싱 실패 → 목업 폴백. 원문: {}", rawJson);
            return mockPriceProvider.getFallbackAnalysis(keyword);
        }

        // Step 3. 카테고리별 KIS 시세 조회 + 병합
        List<StockAnalysisResponse.CategoryDto> result = new ArrayList<>();
        int totalCount = 0;

        for (JsonNode categoryNode : categories) {
            String categoryName = categoryNode.path("category").asText();
            JsonNode stocks     = categoryNode.path("stocks");

            List<StockAnalysisResponse.AnalyzedStockDto> analyzed = new ArrayList<>();
            for (JsonNode stock : stocks) {
                String ticker         = stock.path("ticker").asText("").trim();
                String name           = stock.path("name").asText();
                String reason         = stock.path("reason").asText();
                String recommendation = stock.path("recommendation").asText();

                // Step 4. KIS 실시간 시세 조회 + 병합 (할루시네이션 방어 포함)
                StockAnalysisResponse.AnalyzedStockDto dto =
                        fetchAndMerge(ticker, name, reason, recommendation);

                if (dto != null) {
                    analyzed.add(dto);
                    totalCount++;
                }
            }

            if (!analyzed.isEmpty()) {
                result.add(new StockAnalysisResponse.CategoryDto(categoryName, analyzed));
            }
        }

        return new StockAnalysisResponse(keyword, LocalDateTime.now(), totalCount, result);
    }

    // ── ② Gemini API 호출 ───────────────────────────────────────────────────

    private String callGemini(String keyword) {
        try {
            // system_instruction + user contents 구성
            ObjectNode body = objectMapper.createObjectNode();

            // system_instruction
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ArrayNode systemParts = objectMapper.createArrayNode();
            ObjectNode systemPart = objectMapper.createObjectNode();
            systemPart.put("text", SYSTEM_PROMPT);
            systemParts.add(systemPart);
            systemInstruction.set("parts", systemParts);
            body.set("system_instruction", systemInstruction);

            // contents (user message)
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("role", "user");
            ArrayNode userParts = objectMapper.createArrayNode();
            ObjectNode userPart = objectMapper.createObjectNode();
            userPart.put("text", "키워드: " + keyword);
            userParts.add(userPart);
            userContent.set("parts", userParts);
            contents.add(userContent);
            body.set("contents", contents);

            // generationConfig - thinking 비활성화로 실제 출력 토큰 확보
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("maxOutputTokens", 2048);
            ObjectNode thinkingConfig = objectMapper.createObjectNode();
            thinkingConfig.put("thinkingBudget", 0);
            generationConfig.set("thinkingConfig", thinkingConfig);
            body.set("generationConfig", generationConfig);

            String requestJson = objectMapper.writeValueAsString(body);
            log.debug("[AI분석] 요청 모델={} keyword={}", model, keyword);

            String responseJson = geminiClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestJson)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            res -> res.bodyToMono(String.class).map(errBody -> {
                                log.error("[AI분석] Gemini 오류 응답: {}", errBody);
                                return new RuntimeException("Gemini API 오류: " + errBody);
                            })
                    )
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(40));

            if (responseJson == null) throw new RuntimeException("Gemini API 응답 없음");

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");

            // thinking 파트 제외하고 실제 텍스트 파트만 추출 (gemini-2.5 thinking 모드 대응)
            String text = "";
            for (JsonNode part : parts) {
                if (!part.path("thought").asBoolean(false)) {
                    text = part.path("text").asText();
                    break;
                }
            }
            log.info("[AI분석] LLM 응답 수신 | keyword={} | 길이={}", keyword, text.length());
            return text;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI분석] Gemini API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 분석 서비스 오류", e);
        }
    }

    // ── ③ LLM 응답 JSON 파싱 ────────────────────────────────────────────────

    private JsonNode parseLlmResponse(String rawJson) {
        try {
            String cleaned = rawJson.strip();
            // LLM이 마크다운 코드블록을 감쌌을 경우 방어 처리
            if (cleaned.startsWith("```")) {
                cleaned = cleaned
                        .replaceAll("(?s)^```[a-zA-Z]*\\n?", "")
                        .replaceAll("```\\s*$", "")
                        .strip();
            }
            return objectMapper.readTree(cleaned).path("categories");
        } catch (Exception e) {
            log.error("[AI분석] JSON 파싱 오류: {}", e.getMessage());
            return null;
        }
    }

    // ── ④ KIS 시세 조회 + LLM 분석 결과 병합 ───────────────────────────────

    /**
     * 할루시네이션 방어:
     * - ticker가 6자리 숫자 형식이 아니면 즉시 skip
     * - KIS API가 empty를 반환하면 skip (존재하지 않는 종목코드)
     * - 예외 발생 시 해당 종목만 skip하고 나머지는 계속 처리
     */
    private StockAnalysisResponse.AnalyzedStockDto fetchAndMerge(
            String ticker, String llmName, String reason, String recommendation) {

        // 형식 검증: 6자리 숫자인지 확인
        if (!ticker.matches("\\d{6}")) {
            log.warn("[AI분석] 유효하지 않은 종목코드 skip: '{}'", ticker);
            return null;
        }

        try {
            Optional<KisPriceResponse.Output> opt = stockMarketClient.getCurrentPriceRaw(ticker);

            if (opt.isPresent()) {
                KisPriceResponse.Output o = opt.get();
                String name    = (o.htsName() != null && !o.htsName().isBlank()) ? o.htsName() : llmName;
                long   price   = parseLong(o.currentPrice());
                double rate    = parseDouble(o.changeRate());
                String summary = "[" + recommendation + "] " + reason;
                return new StockAnalysisResponse.AnalyzedStockDto(ticker, name, price, rate, summary);
            }

            // KIS 응답 없음 → 목업 폴백
            log.warn("[AI분석] KIS 시세 없음 → 목업 폴백: {}", ticker);
            return buildFromMock(ticker, llmName, reason, recommendation);

        } catch (Exception e) {
            log.warn("[AI분석] KIS 예외 → 목업 폴백: {} ({})", ticker, e.getMessage());
            return buildFromMock(ticker, llmName, reason, recommendation);
        }
    }

    // ── 목업 폴백 ────────────────────────────────────────────────────────────

    private StockAnalysisResponse.AnalyzedStockDto buildFromMock(
            String ticker, String llmName, String reason, String recommendation) {
        MockStockPriceProvider.MockPrice mock = mockPriceProvider.get(ticker).orElse(null);
        if (mock == null) return null;
        String summary = "[" + recommendation + "] " + reason + " (시세 조회 불가 - 임시 데이터)";
        return new StockAnalysisResponse.AnalyzedStockDto(ticker, llmName, mock.price(), mock.changeRate(), summary);
    }

    // ── 유틸 ────────────────────────────────────────────────────────────────

    private long parseLong(String v) {
        try { return v == null ? 0L : Long.parseLong(v.trim().replace(",", "")); }
        catch (NumberFormatException e) { return 0L; }
    }

    private double parseDouble(String v) {
        try { return v == null ? 0.0 : Double.parseDouble(v.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
