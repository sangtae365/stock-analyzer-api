# Backend — Stock Analyzer

Spring Boot 기반 REST API 서버. KIS OpenAPI 실시간 시세 연동 + Google Gemini AI 키워드 분석 + AI 에이전트 종목 토론 (SSE 스트리밍).

---

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.3 |
| HTTP Client | Spring WebFlux (WebClient) |
| API 문서 | SpringDoc OpenAPI 2.8.6 (Swagger UI) |
| AI | Google Gemini 2.5 Flash |
| 시세 데이터 | KIS(한국투자증권) OpenAPI |
| Build | Maven 3.9 |

---

## 실행

```bash
# API 키 없이 목업 데이터로 실행
mvn spring-boot:run

# application-local.yml에 KIS·Gemini 키 설정 후 실시간 데이터로 실행
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

- 서버 포트: `8080`
- Swagger UI: http://localhost:8080/swagger-ui.html

---

## 환경 설정

`src/main/resources/application-local.yml` 생성 (gitignore 적용):

```yaml
kis:
  app-key: YOUR_KIS_APP_KEY
  app-secret: YOUR_KIS_APP_SECRET

gemini:
  api-key: YOUR_GEMINI_API_KEY
```

`src/main/resources/application.yml` 기본값:

```yaml
kis:
  base-url: https://openapi.koreainvestment.com:9443  # 실전투자
  # base-url: https://openapivts.koreainvestment.com:29443  # 모의투자

gemini:
  model: gemini-2.5-flash
```

---

## API 엔드포인트

### 종목

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/stocks` | 시가총액 상위 종목 목록 |
| GET | `/api/stocks/analyze?keyword={keyword}` | AI 키워드 분석 |
| GET | `/api/stocks/{ticker}` | 종목 상세 조회 |
| GET | `/api/stocks/{ticker}/chart` | 캔들 차트 데이터 |
| GET | `/api/stocks/{ticker}/investor-trend` | 투자자 매매 동향 |
| GET | `/api/stocks/ranking/volume` | 거래량 순위 |
| GET | `/api/stocks/ranking/change-rate` | 등락률 순위 |

### 키워드

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/keywords/popular` | 인기 검색 키워드 |

### AI 토론

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/debate` | 종목 간 AI 토론 시작 (SSE 스트리밍) |

---

## 패키지 구조

```
com.stockanalyzer
├── controller/
│   ├── StockController.java          # 종목 관련 API
│   ├── KeywordController.java        # 인기 키워드 API
│   └── DebateController.java         # AI 토론 API (SSE)
├── service/
│   ├── KisStockAnalysisService.java  # KIS API 오케스트레이션 (Primary)
│   ├── AiStockAnalysisService.java   # Gemini AI 분석
│   ├── MockStockPriceProvider.java   # 목업 폴백 데이터
│   ├── KeywordService.java           # 키워드 관리
│   └── DebateService.java            # AI 토론 오케스트레이션
├── client/
│   ├── StockMarketClient.java        # KIS OpenAPI 클라이언트
│   └── dto/                          # KIS 응답 DTO
├── dto/
│   ├── request/
│   │   └── DebateRequest.java        # 토론 요청 DTO
│   └── response/                     # API 응답 DTO (record)
│       └── DebateEvent.java          # SSE 이벤트 DTO
├── config/
│   ├── CorsConfig.java
│   └── SwaggerConfig.java
└── exception/
    └── KisApiException.java
```

---

## AI 분석 흐름

```
키워드 입력
    │
    ▼
Gemini 2.5 Flash 호출
    │ 실패 시
    ├─────────────────────→ 목업 카테고리 9개 종목 반환
    │
    ▼ 성공
JSON 파싱 (대장주 / 성장 기대주 / 소외주)
    │
    ▼
종목별 KIS 실시간 시세 조회
    │ 실패 시
    ├─────────────────────→ 목업 시세로 대체 (summary에 "임시 데이터" 표시)
    │
    ▼ 성공
최종 분석 결과 반환
```

**할루시네이션 방어:**
- ticker가 6자리 숫자 형식이 아니면 즉시 제외
- KIS에서 존재하지 않는 종목코드 응답 시 제외

---

## AI 토론 흐름

```
POST /api/debate { stocks: ["삼성전자", "SK하이닉스"] }
    │
    ▼  (SSE 스트리밍 시작)
1단계: 종목명 → ticker 변환 (Gemini)
    │  loading 이벤트 전송
    ▼
2단계: KIS 실시간 시세 조회 (현재가, PER/PBR/EPS, 52주 고저가, 외국인 소진율 등)
    │  loading 이벤트 전송
    ▼
3단계: Google Search 뉴스 수집 (종목별 최근 7일 뉴스, Gemini google_search 도구)
    │  loading 이벤트 전송
    ▼
4단계: Gemini 심층 토론 (thinkingBudget=8000, 4라운드)
    │  Round 1: 입장 발표 — 각 지지자의 종목 장점 발표
    │  Round 2: 비판적 검토 — 비평가(critic)의 리스크 분석
    │  Round 3: 교차 반박 — 상대 지지자 주장 직접 반박·비교
    │  Round 4: 최종 평가 — 종합 투자 의견
    │  각 발언마다 message 이벤트 스트리밍
    ▼
5단계: 결론 도출 — 종목별 점수(0~100) + 종합 요약
    │  conclusion 이벤트 전송
    ▼
done 이벤트
```

**SSE 이벤트 타입:**

| type | 설명 |
|---|---|
| `loading` | 준비 단계 진행 상황 메시지 |
| `message` | 에이전트 발언 (agentName, agentRole, targetStock, round, message) |
| `conclusion` | 최종 결과 (scores[], summary) |
| `done` | 스트림 종료 |
| `error` | 오류 발생 |

---

## 목업 폴백

KIS 또는 Gemini 연결 불가 시 `MockStockPriceProvider`가 자동으로 목업 데이터를 제공합니다.

| 상황 | 동작 |
|---|---|
| Gemini 실패 | 사전 정의된 목업 카테고리 9개 종목 반환 |
| KIS 종목 목록 실패 | 목업 시가총액 상위 30개 종목 반환 |
| KIS 개별 시세 실패 | 목업 시세로 대체, summary에 `(임시 데이터)` 표시 |

---

## CORS

`CorsConfig.java`에서 허용 출처:

- `http://localhost:3000`
- `http://localhost:5173`
