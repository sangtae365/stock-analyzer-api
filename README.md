# Backend — Stock Analyzer

Spring Boot 기반 REST API 서버. KIS OpenAPI 실시간 시세 연동 + Google Gemini AI 키워드 분석.

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

---

## 패키지 구조

```
com.stockanalyzer
├── controller/
│   ├── StockController.java          # 종목 관련 API
│   └── KeywordController.java        # 인기 키워드 API
├── service/
│   ├── KisStockAnalysisService.java  # KIS API 오케스트레이션 (Primary)
│   ├── AiStockAnalysisService.java   # Gemini AI 분석
│   ├── MockStockPriceProvider.java   # 목업 폴백 데이터
│   └── KeywordService.java           # 키워드 관리
├── client/
│   ├── StockMarketClient.java        # KIS OpenAPI 클라이언트
│   └── dto/                          # KIS 응답 DTO
├── dto/
│   └── response/                     # API 응답 DTO (record)
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
