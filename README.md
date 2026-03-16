# stock-analyzer-api

주식 섹터 키워드를 입력하면 관련 종목을 AI가 분석해 대장주·성장 기대주·소외주로 분류해주는 서비스의 **백엔드 REST API 서버**입니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 25 LTS |
| Framework | Spring Boot 4.0.3 |
| HTTP Client | Spring WebFlux (WebClient) |
| 외부 API | 한국투자증권 KIS OpenAPI |
| Build | Maven 3.9 |

## 프로젝트 구조

```
src/main/java/com/stockanalyzer/
├── controller/         # REST API 엔드포인트
├── service/            # 비즈니스 로직 (KIS 실시간 / Mock 폴백)
├── client/             # KIS OpenAPI WebClient
│   └── dto/            # KIS 응답 DTO
├── dto/                # 내부 응답 DTO (record)
│   └── response/
├── mock/               # Mock 데이터 (KIS 미연동 시 폴백)
├── exception/          # 커스텀 예외
└── config/             # CORS 설정
```

## API 명세

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/stocks` | 전체 종목 조회 (페이징, 정렬) |
| GET | `/api/stocks/analyze?keyword=` | 키워드 종목 분석 |
| GET | `/api/stocks/{ticker}` | 종목 상세 정보 |
| GET | `/api/keywords/popular` | 인기 검색 키워드 |
| GET | `/api/sectors` | 전체 섹터 목록 |
| GET | `/api/sectors/{sectorId}/stocks` | 섹터별 종목 목록 |

### 응답 예시 — `GET /api/stocks/analyze?keyword=카카오`

```json
{
  "keyword": "카카오",
  "analyzedAt": "2026-03-16T15:00:00",
  "totalCount": 5,
  "categories": [
    {
      "category": "대장주",
      "stocks": [
        {
          "ticker": "377300",
          "name": "카카오페이",
          "price": 24500,
          "changeRate": 3.12,
          "summary": "AI 기반 리스크 분석 고도화로 B2B 확장 중"
        }
      ]
    },
    { "category": "성장 기대주", "stocks": [ "..." ] },
    { "category": "소외주",     "stocks": [ "..." ] }
  ]
}
```

## 실행 방법

### 1. 사전 요구사항

- Java 25 LTS ([Eclipse Temurin 다운로드](https://adoptium.net/))
- Maven 3.9+

### 2. KIS API 키 설정

[KIS Developers](https://apiportal.koreainvestment.com/) 에서 앱 등록 후 키 발급

```bash
# application-local.yml.example 을 복사하여 실제 키 입력
cp src/main/resources/application-local.yml.example \
   src/main/resources/application-local.yml
```

```yaml
# application-local.yml
kis:
  app-key: 발급받은_앱키
  app-secret: 발급받은_앱시크릿
```

### 3. 빌드 및 실행

```bash
# Mock 데이터로 실행 (KIS 키 불필요)
mvn spring-boot:run

# 실제 KIS 데이터로 실행
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

서버 실행 후 → `http://localhost:8080`

### 4. 환경변수로 실행 (운영 환경)

```bash
export KIS_APP_KEY=발급받은_앱키
export KIS_APP_SECRET=발급받은_앱시크릿
java -jar target/stock-analyzer-0.0.1-SNAPSHOT.jar
```

## KIS 키 미설정 시 동작

KIS API 키가 없거나 장애 발생 시 **Mock 데이터로 자동 폴백**되어 서비스가 중단되지 않습니다.

```
KIS API 호출 실패 → WARN 로그 출력 → Mock 데이터 반환
```

## CORS 설정

프론트엔드 주소를 허용합니다 (`CorsConfig.java`):

- `http://localhost:3000`
- `http://localhost:5173`

운영 환경 프론트엔드 주소가 다를 경우 `CorsConfig.java`를 수정하세요.
