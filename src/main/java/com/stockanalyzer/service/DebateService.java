package com.stockanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stockanalyzer.client.StockMarketClient;
import com.stockanalyzer.client.dto.KisPriceResponse;
import com.stockanalyzer.dto.response.DebateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DebateService {

    private static final Logger log = LoggerFactory.getLogger(DebateService.class);

    private final WebClient geminiClient;
    private final StockMarketClient stockMarketClient;
    private final MockStockPriceProvider mockPriceProvider;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 종목별 실시간 데이터를 담는 컨텍스트
    private record StockContext(
            String name,
            String ticker,
            long currentPrice,
            double changeRate,
            double changeAmount,
            double per,
            double pbr,
            double eps,
            long marketCapBil,
            long w52HighPrice,
            long w52LowPrice,
            double w52HighPriceRate,
            double w52LowPriceRate,
            double foreignExhaustionRate,
            String sectorName,
            boolean isLiveData,
            String recentNews  // 최근 1주일 뉴스 요약
    ) {}

    public DebateService(
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

    // ── 공개 API ─────────────────────────────────────────────────────────────

    public SseEmitter startDebate(List<String> stockNames) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3분 타임아웃 (thinking 포함)

        executor.submit(() -> {
            try {
                // Step 1. ticker 해석
                emit(emitter, DebateEvent.loading("종목 코드 조회 중..."));
                List<StockContext> contexts = resolveStocks(stockNames);

                // Step 2. KIS 실시간 시세 조회
                emit(emitter, DebateEvent.loading("KIS 실시간 시세 조회 중..."));
                contexts = enrichWithKisData(contexts);

                // Step 2.5. 종목별 최신 뉴스 검색 (Google Search 그라운딩)
                List<StockContext> contextsWithNews = new ArrayList<>();
                for (StockContext ctx : contexts) {
                    emit(emitter, DebateEvent.loading("'" + ctx.name() + "' 최신 뉴스 검색 중..."));
                    String news = fetchRecentNews(ctx.name());
                    contextsWithNews.add(new StockContext(
                            ctx.name(), ctx.ticker(),
                            ctx.currentPrice(), ctx.changeRate(), ctx.changeAmount(),
                            ctx.per(), ctx.pbr(), ctx.eps(), ctx.marketCapBil(),
                            ctx.w52HighPrice(), ctx.w52LowPrice(),
                            ctx.w52HighPriceRate(), ctx.w52LowPriceRate(),
                            ctx.foreignExhaustionRate(), ctx.sectorName(), ctx.isLiveData(),
                            news
                    ));
                }
                contexts = contextsWithNews;

                // Step 3. Gemini 깊은 토론 생성
                String dataReport = buildDataReport(contexts);
                emit(emitter, DebateEvent.loading("AI 에이전트 토론 생성 중... (깊은 분석 중, 30초~1분 소요)"));
                String rawJson = callGeminiDeep(stockNames, contexts, dataReport);
                JsonNode root = parseDebateResponse(rawJson);

                if (root == null) {
                    emit(emitter, DebateEvent.error("AI 토론 스크립트 생성에 실패했습니다."));
                    emitter.complete();
                    return;
                }

                // Step 4. 라운드별 메시지 순차 스트리밍
                for (JsonNode roundNode : root.path("rounds")) {
                    int round = roundNode.path("round").asInt();
                    String roundName = roundNode.path("roundName").asText();

                    for (JsonNode msg : roundNode.path("messages")) {
                        emit(emitter, DebateEvent.message(
                                msg.path("agentName").asText(),
                                msg.path("agentRole").asText(),
                                msg.path("targetStock").asText(""),
                                round,
                                roundName,
                                msg.path("message").asText()
                        ));
                        Thread.sleep(700);
                    }
                }

                // Step 5. 결론 이벤트 (실제 시세 기반 스코어 포함)
                JsonNode conclusionNode = root.path("conclusion");
                List<DebateEvent.ScoreDto> scores = new ArrayList<>();
                for (JsonNode scoreNode : conclusionNode.path("scores")) {
                    scores.add(new DebateEvent.ScoreDto(
                            scoreNode.path("name").asText(),
                            scoreNode.path("ticker").asText(),
                            scoreNode.path("score").asInt(),
                            scoreNode.path("reason").asText()
                    ));
                }
                emit(emitter, DebateEvent.conclusion(scores, conclusionNode.path("summary").asText()));
                emit(emitter, DebateEvent.done());
                emitter.complete();

            } catch (Exception e) {
                log.error("[토론] 오류 발생: {}", e.getMessage(), e);
                try {
                    emit(emitter, DebateEvent.error("토론 중 오류가 발생했습니다."));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    // ── Step 1: 종목명 → ticker 해석 (Gemini 활용) ───────────────────────────

    private List<StockContext> resolveStocks(List<String> stockNames) {
        String prompt = "다음 한국 주식 종목명의 코스피/코스닥 6자리 종목코드를 JSON으로만 반환하세요. "
                + "설명 없이 JSON만 출력. 형식: [{\"name\":\"종목명\",\"ticker\":\"000000\"},...]\n"
                + "종목명: " + String.join(", ", stockNames);

        try {
            String response = callGeminiSimple(prompt);
            String cleaned = response.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("(?s)^```[a-zA-Z]*\\n?", "").replaceAll("```\\s*$", "").strip();
            }
            JsonNode arr = objectMapper.readTree(cleaned);
            List<StockContext> result = new ArrayList<>();
            for (JsonNode node : arr) {
                result.add(new StockContext(
                        node.path("name").asText(),
                        node.path("ticker").asText(),
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "미확인", false, ""
                ));
            }
            if (!result.isEmpty()) return result;
        } catch (Exception e) {
            log.warn("[토론] ticker 해석 실패, 이름만으로 진행: {}", e.getMessage());
        }

        // 폴백: ticker 없이 이름만으로 구성
        return stockNames.stream()
                .map(name -> new StockContext(name, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "미확인", false, ""))
                .toList();
    }

    // ── Step 2: KIS 실시간 시세로 컨텍스트 보강 ─────────────────────────────

    private List<StockContext> enrichWithKisData(List<StockContext> contexts) {
        List<StockContext> enriched = new ArrayList<>();
        for (StockContext ctx : contexts) {
            if (ctx.ticker().isBlank() || !ctx.ticker().matches("\\d{6}")) {
                enriched.add(ctx);
                continue;
            }
            try {
                Optional<KisPriceResponse.Output> opt = stockMarketClient.getCurrentPriceRaw(ctx.ticker());
                if (opt.isPresent()) {
                    KisPriceResponse.Output o = opt.get();
                    enriched.add(new StockContext(
                            o.htsName() != null && !o.htsName().isBlank() ? o.htsName() : ctx.name(),
                            ctx.ticker(),
                            parseLong(o.currentPrice()),
                            parseDouble(o.changeRate()),
                            parseDouble(o.changeAmount()),
                            parseDouble(o.per()),
                            parseDouble(o.pbr()),
                            parseDouble(o.eps()),
                            parseLong(o.marketCapBil()),
                            parseLong(o.w52HighPrice()),
                            parseLong(o.w52LowPrice()),
                            parseDouble(o.w52HighPriceRate()),
                            parseDouble(o.w52LowPriceRate()),
                            parseDouble(o.foreignExhaustionRate()),
                            o.sectorName() != null ? o.sectorName() : "미확인",
                            true,
                            ctx.recentNews()
                    ));
                    log.info("[토론] KIS 시세 조회 성공: {} ({}원, {}%)", ctx.name(), parseLong(o.currentPrice()), parseDouble(o.changeRate()));
                    continue;
                }
            } catch (Exception e) {
                log.warn("[토론] KIS 조회 실패 [{}]: {}", ctx.ticker(), e.getMessage());
            }
            // KIS 실패 시 mock 폴백
            MockStockPriceProvider.MockPrice mock = mockPriceProvider.get(ctx.ticker()).orElse(null);
            if (mock != null) {
                enriched.add(new StockContext(ctx.name(), ctx.ticker(), mock.price(), mock.changeRate(),
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "미확인", false, ctx.recentNews()));
            } else {
                enriched.add(ctx);
            }
        }
        return enriched;
    }

    // ── Step 2.5: 최신 뉴스 검색 (Gemini + Google Search 그라운딩) ──────────

    private String fetchRecentNews(String stockName) {
        try {
            ObjectNode body = objectMapper.createObjectNode();

            // Google Search 도구 설정
            ArrayNode tools = objectMapper.createArrayNode();
            ObjectNode searchTool = objectMapper.createObjectNode();
            searchTool.set("google_search", objectMapper.createObjectNode());
            tools.add(searchTool);
            body.set("tools", tools);

            // contents
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("role", "user");
            ArrayNode userParts = objectMapper.createArrayNode();
            ObjectNode userPart = objectMapper.createObjectNode();
            userPart.put("text",
                    "최근 7일 이내 한국 주식 종목 '" + stockName + "'에 관한 주요 뉴스와 이슈를 검색하여 요약해주세요. "
                    + "주가에 영향을 줄 수 있는 수주·실적·계약·규제·신제품·경영진 변화·업종 트렌드 등을 중심으로 "
                    + "bullet point 3~5개로 간결하게 한국어로 작성해주세요. "
                    + "검색 결과가 없거나 특별한 이슈가 없으면 '최근 특이 뉴스 없음'으로만 답하세요.");
            userParts.add(userPart);
            userContent.set("parts", userParts);
            contents.add(userContent);
            body.set("contents", contents);

            // thinking 비활성화 (검색 도구와 동시 사용)
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("maxOutputTokens", 1024);
            body.set("generationConfig", generationConfig);

            String responseJson = geminiClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));

            if (responseJson == null) return "뉴스 조회 실패";

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");
            for (JsonNode part : parts) {
                String text = part.path("text").asText("").strip();
                if (!text.isEmpty()) {
                    log.info("[토론] 뉴스 검색 완료: {} | 길이={}", stockName, text.length());
                    return text;
                }
            }
            return "최근 특이 뉴스 없음";

        } catch (Exception e) {
            log.warn("[토론] 뉴스 검색 실패 [{}]: {}", stockName, e.getMessage());
            return "뉴스 검색 불가 (AI 자체 지식 기반으로 분석)";
        }
    }

    // ── Step 3: 실데이터 보고서 텍스트 생성 ─────────────────────────────────

    private String buildDataReport(List<StockContext> contexts) {
        StringBuilder sb = new StringBuilder();
        sb.append("[토론 대상 종목 실시간 데이터 - KIS 기준]\n\n");
        for (StockContext ctx : contexts) {
            sb.append("■ ").append(ctx.name());
            if (!ctx.ticker().isBlank()) sb.append(" (").append(ctx.ticker()).append(")");
            sb.append("\n");
            if (ctx.isLiveData()) {
                sb.append("  · 현재가: ").append(String.format("%,d", ctx.currentPrice())).append("원");
                sb.append("  (전일대비 ").append(ctx.changeRate() >= 0 ? "+" : "").append(String.format("%.2f", ctx.changeRate())).append("%)\n");
                if (ctx.per() > 0)
                    sb.append("  · PER: ").append(String.format("%.1f", ctx.per()))
                      .append("  PBR: ").append(String.format("%.2f", ctx.pbr()))
                      .append("  EPS: ").append(String.format("%,d", (long)ctx.eps())).append("원\n");
                if (ctx.marketCapBil() > 0)
                    sb.append("  · 시가총액: ").append(String.format("%,d", ctx.marketCapBil())).append("억원\n");
                if (ctx.w52HighPrice() > 0)
                    sb.append("  · 52주 최고: ").append(String.format("%,d", ctx.w52HighPrice())).append("원")
                      .append("  (현재가 대비 ").append(String.format("%.1f", ctx.w52HighPriceRate())).append("%)\n");
                if (ctx.w52LowPrice() > 0)
                    sb.append("  · 52주 최저: ").append(String.format("%,d", ctx.w52LowPrice())).append("원")
                      .append("  (현재가 대비 +").append(String.format("%.1f", ctx.w52LowPriceRate())).append("%)\n");
                if (ctx.foreignExhaustionRate() > 0)
                    sb.append("  · 외국인 소진율: ").append(String.format("%.1f", ctx.foreignExhaustionRate())).append("%\n");
                if (!ctx.sectorName().equals("미확인"))
                    sb.append("  · 업종: ").append(ctx.sectorName()).append("\n");
            } else {
                sb.append("  · 실시간 시세 조회 불가 (AI 자체 지식 기반으로 분석)\n");
            }
            // 최신 뉴스
            if (ctx.recentNews() != null && !ctx.recentNews().isBlank()) {
                sb.append("  [최근 1주일 뉴스]\n");
                for (String line : ctx.recentNews().split("\n")) {
                    if (!line.isBlank()) sb.append("  ").append(line.strip()).append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ── Gemini 깊은 토론 호출 (thinking 활성화) ──────────────────────────────

    private String callGeminiDeep(List<String> stockNames, List<StockContext> contexts, String dataReport) {
        try {
            ObjectNode body = objectMapper.createObjectNode();

            // system_instruction
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ArrayNode systemParts = objectMapper.createArrayNode();
            ObjectNode systemPart = objectMapper.createObjectNode();
            systemPart.put("text", buildSystemPrompt(stockNames, contexts));
            systemParts.add(systemPart);
            systemInstruction.set("parts", systemParts);
            body.set("system_instruction", systemInstruction);

            // contents (user message with real data)
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("role", "user");
            ArrayNode userParts = objectMapper.createArrayNode();
            ObjectNode userPart = objectMapper.createObjectNode();
            userPart.put("text", dataReport + "\n위 실시간 데이터를 바탕으로 깊이 있는 투자 토론을 진행해주세요.");
            userParts.add(userPart);
            userContent.set("parts", userParts);
            contents.add(userContent);
            body.set("contents", contents);

            // generationConfig - thinking 활성화로 깊은 분석
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("maxOutputTokens", 8192);
            ObjectNode thinkingConfig = objectMapper.createObjectNode();
            thinkingConfig.put("thinkingBudget", 8000);
            generationConfig.set("thinkingConfig", thinkingConfig);
            body.set("generationConfig", generationConfig);

            String requestJson = objectMapper.writeValueAsString(body);
            log.info("[토론] Gemini 깊은 분석 호출 | 종목={}", stockNames);

            String responseJson = geminiClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestJson)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            res -> res.bodyToMono(String.class).map(errBody -> {
                                log.error("[토론] Gemini 오류: {}", errBody);
                                return new RuntimeException("Gemini API 오류: " + errBody);
                            })
                    )
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(150)); // thinking 포함 넉넉히

            if (responseJson == null) throw new RuntimeException("Gemini 응답 없음");

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");

            // thinking 파트 제외하고 실제 텍스트만 추출
            for (JsonNode part : parts) {
                if (!part.path("thought").asBoolean(false)) {
                    String text = part.path("text").asText();
                    log.info("[토론] 응답 수신 | 길이={}", text.length());
                    return text;
                }
            }
            throw new RuntimeException("유효한 응답 파트 없음");

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[토론] Gemini 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 토론 서비스 오류", e);
        }
    }

    // ── Gemini 간단 호출 (ticker 해석용, thinking 비활성화) ────────────────

    private String callGeminiSimple(String userMessage) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("role", "user");
            ArrayNode userParts = objectMapper.createArrayNode();
            ObjectNode userPart = objectMapper.createObjectNode();
            userPart.put("text", userMessage);
            userParts.add(userPart);
            userContent.set("parts", userParts);
            contents.add(userContent);
            body.set("contents", contents);

            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("maxOutputTokens", 512);
            ObjectNode thinkingConfig = objectMapper.createObjectNode();
            thinkingConfig.put("thinkingBudget", 0);
            generationConfig.set("thinkingConfig", thinkingConfig);
            body.set("generationConfig", generationConfig);

            String responseJson = geminiClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(15));

            if (responseJson == null) throw new RuntimeException("응답 없음");
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");
            for (JsonNode part : parts) {
                if (!part.path("thought").asBoolean(false)) return part.path("text").asText();
            }
            throw new RuntimeException("응답 파트 없음");
        } catch (Exception e) {
            throw new RuntimeException("Gemini 간단 호출 실패: " + e.getMessage(), e);
        }
    }

    // ── 시스템 프롬프트 생성 ────────────────────────────────────────────────

    private String buildSystemPrompt(List<String> stockNames, List<StockContext> contexts) {
        StringBuilder agents = new StringBuilder();
        for (StockContext ctx : contexts) {
            agents.append("- ").append(ctx.name()).append(" 지지자 (전담 애널리스트):\n")
                  .append("  * ").append(ctx.name()).append(" 종목의 강력한 매수 논거를 제시합니다.\n")
                  .append("  * 제공된 실시간 재무 데이터(현재가, PER, PBR, 52주 고저가, 외국인소진율)를 반드시 인용합니다.\n")
                  .append("  * 업종 내 경쟁 우위, 성장 촉매(Catalyst), 밸류에이션 매력도를 구체적으로 분석합니다.\n")
                  .append("  * 상대 종목 대비 우위 포인트를 논리적으로 주장합니다.\n\n");
        }
        agents.append("- 비판적 검토자 (리스크 매니저):\n")
              .append("  * 각 종목의 약점과 리스크를 날카롭게 지적합니다.\n")
              .append("  * 실시간 데이터에서 보이는 위험 신호(고평가, 52주 고점 근접, 외국인 이탈 등)를 근거로 활용합니다.\n")
              .append("  * 어느 한 종목에 편향되지 않고 균형 잡힌 시각을 유지합니다.");

        StringBuilder stocksSchema = new StringBuilder("[");
        StringBuilder scoresSchema = new StringBuilder("[");
        for (int i = 0; i < contexts.size(); i++) {
            if (i > 0) { stocksSchema.append(","); scoresSchema.append(","); }
            String name = contexts.get(i).name();
            String ticker = contexts.get(i).ticker();
            stocksSchema.append("{\"name\":\"").append(name).append("\",\"ticker\":\"").append(ticker).append("\"}");
            scoresSchema.append("{\"name\":\"").append(name).append("\",\"ticker\":\"").append(ticker)
                        .append("\",\"score\":50,\"reason\":\"구체적 한 줄 근거\"}");
        }
        stocksSchema.append("]");
        scoresSchema.append("]");

        // Round 3용 상대 종목 비교 지침을 동적으로 생성
        StringBuilder round3Rules = new StringBuilder();
        for (int i = 0; i < contexts.size(); i++) {
            String me = contexts.get(i).name();
            StringBuilder opponents = new StringBuilder();
            for (int j = 0; j < contexts.size(); j++) {
                if (i != j) {
                    if (opponents.length() > 0) opponents.append(", ");
                    opponents.append(contexts.get(j).name());
                }
            }
            round3Rules.append("  · ").append(me).append(" 지지자: ")
                       .append(opponents).append(" 지지자가 Round 1에서 한 주장 중 약점을 정면으로 지적하고, ")
                       .append("'").append(opponents).append("의 [특정 주장]은 사실 [반박 근거]이며, ")
                       .append("반면 ").append(me).append("은 [비교 우위]가 있다'는 구조로 발언합니다.\n");
        }

        return "당신은 한국 주식 시장 전문 투자 토론 AI입니다. "
             + "실시간 KIS 시세 데이터와 최신 뉴스를 기반으로 각 에이전트가 심층 분석하여 토론을 진행합니다.\n\n"
             + "[에이전트 구성 및 역할]\n"
             + agents + "\n\n"
             + "[토론 품질 기준]\n"
             + "- 실제 재무 데이터(현재가, PER, PBR, EPS, 시총, 52주 고저가, 외국인소진율)와 최신 뉴스를 수치로 인용하여 발언합니다.\n"
             + "- 단순 긍정/부정이 아닌, 구체적인 숫자와 업종 트렌드 근거를 제시합니다.\n"
             + "- 각 발언은 3~5문장으로 작성하며, 전문적이고 자연스러운 한국어 구어체를 사용합니다.\n"
             + "- 투자 판단 시 성장성, 밸류에이션, 기술적 지표, 외국인/기관 수급을 종합 고려합니다.\n"
             + "- 최종 결론의 score 합계는 반드시 100이어야 하며, 실데이터 근거로 판단합니다.\n\n"
             + "[토론 구조 - 정확히 4라운드]\n"
             + "Round 1 '입장 발표': 각 종목 지지자가 실데이터·뉴스 기반 매수 근거를 제시합니다. (자기 종목 중심)\n"
             + "Round 2 '비판적 검토': 비판적 검토자가 각 종목의 리스크와 약점을 구체적 수치와 함께 지적합니다.\n"
             + "Round 3 '교차 반박': ★ 이 라운드는 상대 종목과의 직접 비교가 핵심입니다.\n"
             + round3Rules
             + "  규칙: Round 1·2에서 나온 상대방의 구체적 주장을 인용하고, 그 주장의 허점을 논리적으로 공략해야 합니다.\n"
             + "  자기 종목의 장점만 반복하는 발언은 금지입니다. 반드시 상대 종목을 직접 언급하고 비교해야 합니다.\n"
             + "Round 4 '최종 평가': 비판적 검토자가 3라운드의 교차 반박까지 종합하여 최종 투자 우선순위를 판단합니다.\n\n"
             + "[출력 규칙]\n"
             + "- 오직 JSON만 출력합니다. 마크다운 코드블록, 설명 문장 절대 금지.\n"
             + "- 첫 글자는 반드시 { 이어야 합니다.\n\n"
             + "[출력 JSON 스키마]\n"
             + "{\"stocks\":" + stocksSchema
             + ",\"rounds\":["
             + "{\"round\":1,\"roundName\":\"입장 발표\",\"messages\":[{\"agentName\":\"에이전트명\",\"agentRole\":\"advocate\",\"targetStock\":\"종목명\",\"message\":\"실데이터·뉴스 기반 매수 근거\"}]},"
             + "{\"round\":2,\"roundName\":\"비판적 검토\",\"messages\":[{\"agentName\":\"비판적 검토자\",\"agentRole\":\"critic\",\"targetStock\":\"\",\"message\":\"각 종목의 구체적 리스크 지적\"}]},"
             + "{\"round\":3,\"roundName\":\"교차 반박\",\"messages\":[{\"agentName\":\"에이전트명\",\"agentRole\":\"advocate\",\"targetStock\":\"종목명\",\"message\":\"상대 종목 지지자의 주장을 직접 인용·반박하고 자기 종목의 비교 우위를 제시\"}]},"
             + "{\"round\":4,\"roundName\":\"최종 평가\",\"messages\":[{\"agentName\":\"비판적 검토자\",\"agentRole\":\"critic\",\"targetStock\":\"\",\"message\":\"교차 반박까지 종합한 최종 투자 우선순위 판단\"}]}"
             + "],\"conclusion\":{\"scores\":" + scoresSchema
             + ",\"summary\":\"실데이터·뉴스·교차 반박을 종합한 결론 3~4문장\"}}\n";
    }

    // ── JSON 파싱 ────────────────────────────────────────────────────────────

    private JsonNode parseDebateResponse(String rawJson) {
        try {
            String cleaned = rawJson.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned
                        .replaceAll("(?s)^```[a-zA-Z]*\\n?", "")
                        .replaceAll("```\\s*$", "")
                        .strip();
            }
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("[토론] JSON 파싱 오류: {}", e.getMessage());
            return null;
        }
    }

    // ── SSE 전송 유틸 ────────────────────────────────────────────────────────

    private void emit(SseEmitter emitter, DebateEvent event) throws IOException {
        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
    }

    // ── 숫자 파싱 유틸 ──────────────────────────────────────────────────────

    private long parseLong(String v) {
        try { return v == null ? 0L : Long.parseLong(v.trim().replace(",", "")); }
        catch (NumberFormatException e) { return 0L; }
    }

    private double parseDouble(String v) {
        try { return v == null ? 0.0 : Double.parseDouble(v.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
