package com.stockanalyzer.client;

import com.stockanalyzer.client.dto.KisPriceResponse;
import com.stockanalyzer.client.dto.KisTokenRequest;
import com.stockanalyzer.client.dto.KisTokenResponse;
import com.stockanalyzer.dto.StockDto;
import com.stockanalyzer.exception.KisApiException;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class StockMarketClient {

    private static final Logger log = LoggerFactory.getLogger(StockMarketClient.class);

    private final WebClient webClient;
    private final String appKey;
    private final String appSecret;

    // 토큰 캐시 (24시간 유효)
    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.MIN;

    public StockMarketClient(
            @Value("${kis.base-url}")          String baseUrl,
            @Value("${kis.app-key}")           String appKey,
            @Value("${kis.app-secret}")        String appSecret,
            @Value("${kis.timeout.connect-ms}") int connectMs,
            @Value("${kis.timeout.read-ms}")    int readMs
    ) {
        this.appKey    = appKey;
        this.appSecret = appSecret;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectMs)
                .responseTimeout(Duration.ofMillis(readMs));

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    // ── 토큰 ───────────────────────────────────────────────────────────────

    private synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        log.info("KIS 액세스 토큰 갱신 중...");
        try {
            KisTokenResponse response = webClient.post()
                    .uri("/oauth2/tokenP")
                    .bodyValue(KisTokenRequest.of(appKey, appSecret))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            res -> res.bodyToMono(String.class)
                                    .map(body -> new KisApiException("토큰 발급 실패: " + body, res.statusCode().value())))
                    .bodyToMono(KisTokenResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || response.accessToken() == null) {
                throw new KisApiException("토큰 응답이 비어 있습니다.", 500);
            }

            cachedToken  = response.accessToken();
            tokenExpiry  = Instant.now().plusSeconds(response.expiresIn() - 60); // 만료 1분 전 갱신
            log.info("KIS 토큰 갱신 완료. 만료: {}", tokenExpiry);
            return cachedToken;

        } catch (KisApiException e) {
            throw e;
        } catch (Exception e) {
            throw new KisApiException("KIS 토큰 발급 중 오류 발생", e);
        }
    }

    // ── 현재가 조회 ────────────────────────────────────────────────────────

    /**
     * 종목 현재가 조회 (KIS FHKST01010100)
     * @param ticker 종목 코드 (예: "005930")
     * @return StockDto (조회 실패 시 empty)
     */
    public Optional<StockDto> getCurrentPrice(String ticker, String sector) {
        try {
            String token = getAccessToken();

            KisPriceResponse response = webClient.get()
                    .uri(uri -> uri
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", ticker)
                            .build())
                    .header("authorization", "Bearer " + token)
                    .header("appkey",    appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id",     "FHKST01010100")
                    .header("custtype",  "P")
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            res -> res.bodyToMono(String.class)
                                    .map(body -> new KisApiException("현재가 조회 실패 [" + ticker + "]: " + body, res.statusCode().value())))
                    .bodyToMono(KisPriceResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || !response.isSuccess() || response.output() == null) {
                log.warn("KIS 응답 비정상 [{}]: {}", ticker, response != null ? response.message() : "null");
                return Optional.empty();
            }

            return Optional.of(mapToStockDto(response.output(), ticker, sector));

        } catch (KisApiException e) {
            log.error("KIS API 오류 [{}]: {}", ticker, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("현재가 조회 중 예외 [{}]: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    // ── 매핑 ──────────────────────────────────────────────────────────────

    private StockDto mapToStockDto(KisPriceResponse.Output output, String ticker, String sector) {
        long price     = parseLong(output.currentPrice());
        double change  = parseDouble(output.changeRate());
        long marketCap = parseLong(output.marketCapBil()) * 100_000_000L; // 억 → 원

        String name = (output.name() != null && !output.name().isBlank())
                ? output.name() : ticker;

        return new StockDto(ticker, name, price, change, marketCap, sector);
    }

    private long parseLong(String value) {
        try { return value == null ? 0L : Long.parseLong(value.trim().replace(",", "")); }
        catch (NumberFormatException e) { return 0L; }
    }

    private double parseDouble(String value) {
        try { return value == null ? 0.0 : Double.parseDouble(value.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
