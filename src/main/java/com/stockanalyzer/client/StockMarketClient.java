package com.stockanalyzer.client;

import com.stockanalyzer.client.dto.KisChangeRateRankResponse;
import com.stockanalyzer.client.dto.KisChartResponse;
import com.stockanalyzer.client.dto.KisInvestorTrendResponse;
import com.stockanalyzer.client.dto.KisMarketCapRankResponse;
import com.stockanalyzer.client.dto.KisPriceResponse;
import com.stockanalyzer.client.dto.KisStockInfoResponse;
import com.stockanalyzer.client.dto.KisTokenRequest;
import com.stockanalyzer.client.dto.KisTokenResponse;
import com.stockanalyzer.client.dto.KisVolumeRankResponse;
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

        log.info("KIS 클라이언트 초기화 | base-url={} | app-key={}...{} | app-secret={}...{}",
                baseUrl,
                appKey.substring(0, Math.min(4, appKey.length())),
                appKey.substring(Math.max(0, appKey.length() - 4)),
                appSecret.substring(0, Math.min(4, appSecret.length())),
                appSecret.substring(Math.max(0, appSecret.length() - 4)));

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectMs)
                .responseTimeout(Duration.ofMillis(readMs));

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
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

    // ── 기간별 차트 조회 (FHKST03010100) ─────────────────────────────────

    /**
     * 국내주식 기간별시세 차트 조회
     * @param ticker     종목코드
     * @param startDate  시작일 (YYYYMMDD)
     * @param endDate    종료일 (YYYYMMDD)
     * @param period     D:일봉 W:주봉 M:월봉 Y:년봉
     */
    public Optional<KisChartResponse> getChartData(String ticker, String startDate, String endDate, String period) {
        try {
            String token = getAccessToken();
            KisChartResponse response = webClient.get()
                    .uri(uri -> uri
                            .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", ticker)
                            .queryParam("FID_INPUT_DATE_1", startDate)
                            .queryParam("FID_INPUT_DATE_2", endDate)
                            .queryParam("FID_PERIOD_DIV_CODE", period)
                            .queryParam("FID_ORG_ADJ_PRC", "0")
                            .build())
                    .header("authorization", "Bearer " + token)
                    .header("appkey",    appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id",     "FHKST03010100")
                    .header("custtype",  "P")
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            res -> res.bodyToMono(String.class)
                                    .map(body -> new KisApiException("차트 조회 실패 [" + ticker + "]: " + body, res.statusCode().value())))
                    .bodyToMono(KisChartResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || !response.isSuccess()) {
                log.warn("KIS 차트 응답 비정상 [{}]: {}", ticker, response != null ? response.message() : "null");
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (Exception e) {
            log.error("차트 조회 중 예외 [{}]: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    // ── 주식기본조회 (CTPF1002R) ─────────────────────────────────────────
    // 실전 전용 (모의투자 미지원)

    /**
     * 주식기본정보 조회 (상장일, 업종코드, 주식종류 등)
     */
    public Optional<KisStockInfoResponse> getStockInfo(String ticker) {
        try {
            String token = getAccessToken();
            KisStockInfoResponse response = webClient.get()
                    .uri(uri -> uri
                            .path("/uapi/domestic-stock/v1/quotations/search-stock-info")
                            .queryParam("PRDT_TYPE_CD", "300")
                            .queryParam("PDNO", ticker)
                            .build())
                    .header("authorization", "Bearer " + token)
                    .header("appkey",    appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id",     "CTPF1002R")
                    .header("custtype",  "P")
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            res -> res.bodyToMono(String.class)
                                    .map(body -> new KisApiException("주식기본조회 실패 [" + ticker + "]: " + body, res.statusCode().value())))
                    .bodyToMono(KisStockInfoResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || !response.isSuccess()) {
                log.warn("KIS 주식기본조회 비정상 [{}]: {}", ticker, response != null ? response.message() : "null");
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (Exception e) {
            log.error("주식기본조회 중 예외 [{}]: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    // ── 투자자매매동향 (FHPTJ04160001) ───────────────────────────────────
    // 실전 전용, 당일 데이터는 15:40 이후 가능

    /**
     * 종목별 투자자매매동향(일별) 조회
     * @param ticker 종목코드
     * @param date   조회 일자 (YYYYMMDD)
     */
    public Optional<KisInvestorTrendResponse> getInvestorTrend(String ticker, String date) {
        try {
            String token = getAccessToken();
            KisInvestorTrendResponse response = webClient.get()
                    .uri(uri -> uri
                            .path("/uapi/domestic-stock/v1/quotations/investor-trend-estimate")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", ticker)
                            .queryParam("FID_INPUT_DATE_1", date)
                            .queryParam("FID_ORG_ADJ_PRC", "")
                            .queryParam("FID_ETC_CLS_CODE", "1")
                            .build())
                    .header("authorization", "Bearer " + token)
                    .header("appkey",    appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id",     "FHPTJ04160001")
                    .header("custtype",  "P")
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            res -> res.bodyToMono(String.class)
                                    .map(body -> new KisApiException("투자자매매동향 조회 실패 [" + ticker + "]: " + body, res.statusCode().value())))
                    .bodyToMono(KisInvestorTrendResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || !response.isSuccess()) {
                log.warn("KIS 투자자매매동향 비정상 [{}]: {}", ticker, response != null ? response.message() : "null");
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (Exception e) {
            log.error("투자자매매동향 조회 중 예외 [{}]: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    // ── 시가총액 순위 (FHPST01740000) ────────────────────────────────────
    // 실전 전용, 최대 30건

    /**
     * 시가총액 순위 조회
     * @param divCode 0:전체, 1:보통주, 2:우선주
     */
    public Optional<KisMarketCapRankResponse> getMarketCapRanking(String divCode) {
        try {
            String token = getAccessToken();
            KisMarketCapRankResponse response = webClient.get()
                    .uri(uri -> uri
                            .path("/uapi/domestic-stock/v1/ranking/market-cap")
                            .queryParam("fid_cond_mrkt_div_code", "J")
                            .queryParam("fid_cond_scr_div_code",  "20174")
                            .queryParam("fid_div_cls_code",        divCode)
                            .queryParam("fid_input_iscd",          "0000")
                            .queryParam("fid_trgt_cls_code",       "0")
                            .queryParam("fid_trgt_exls_cls_code",  "0")
                            .queryParam("fid_input_price_1", "")
                            .queryParam("fid_input_price_2", "")
                            .queryParam("fid_vol_cnt",       "")
                            .build())
                    .header("authorization", "Bearer " + token)
                    .header("appkey",    appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id",     "FHPST01740000")
                    .header("custtype",  "P")
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            res -> res.bodyToMono(String.class)
                                    .map(body -> new KisApiException("시가총액순위 조회 실패: " + body, res.statusCode().value())))
                    .bodyToMono(KisMarketCapRankResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || !response.isSuccess()) {
                log.warn("KIS 시가총액순위 비정상: {}", response != null ? response.message() : "null");
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (Exception e) {
            log.error("시가총액순위 조회 중 예외: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ── 거래량 순위 (FHPST01710000) ──────────────────────────────────────
    // 실전 전용, 최대 30건

    /**
     * 거래량 순위 조회
     * @param marketCode J:전체, 0001:코스피, 1001:코스닥
     */
    public Optional<KisVolumeRankResponse> getVolumeRanking(String marketCode) {
        try {
            String token = getAccessToken();
            KisVolumeRankResponse response = webClient.get()
                    .uri(uri -> uri
                            .path("/uapi/domestic-stock/v1/quotations/volume-rank")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_COND_SCR_DIV_CODE",  "20171")
                            .queryParam("FID_INPUT_ISCD", marketCode)
                            .queryParam("FID_DIV_CLS_CODE",       "0")
                            .queryParam("FID_BLNG_CLS_CODE",      "0")
                            .queryParam("FID_TRGT_CLS_CODE",      "111111111")
                            .queryParam("FID_TRGT_EXLS_CLS_CODE", "0000000000")
                            .queryParam("FID_INPUT_PRICE_1", "")
                            .queryParam("FID_INPUT_PRICE_2", "")
                            .queryParam("FID_VOL_CNT",       "")
                            .queryParam("FID_INPUT_DATE_1",  "")
                            .build())
                    .header("authorization", "Bearer " + token)
                    .header("appkey",    appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id",     "FHPST01710000")
                    .header("custtype",  "P")
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            res -> res.bodyToMono(String.class)
                                    .map(body -> new KisApiException("거래량순위 조회 실패: " + body, res.statusCode().value())))
                    .bodyToMono(KisVolumeRankResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || !response.isSuccess()) {
                log.warn("KIS 거래량순위 비정상: {}", response != null ? response.message() : "null");
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (Exception e) {
            log.error("거래량순위 조회 중 예외: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ── 등락률 순위 (FHPST01700000) ──────────────────────────────────────
    // 실전 전용, 최대 30건

    /**
     * 등락률 순위 조회
     * @param sortCode 0:상승율순 1:하락율순
     * @param marketCode 0000:전체, 0001:코스피, 1001:코스닥
     */
    public Optional<KisChangeRateRankResponse> getChangeRateRanking(String sortCode, String marketCode) {
        try {
            String token = getAccessToken();
            KisChangeRateRankResponse response = webClient.get()
                    .uri(uri -> uri
                            .path("/uapi/domestic-stock/v1/ranking/fluctuation")
                            .queryParam("fid_cond_mrkt_div_code", "J")
                            .queryParam("fid_cond_scr_div_code",  "20170")
                            .queryParam("fid_input_iscd",         marketCode)
                            .queryParam("fid_rank_sort_cls_code", sortCode)
                            .queryParam("fid_input_cnt_1",        "0")
                            .queryParam("fid_prc_cls_code",       "1")
                            .queryParam("fid_input_price_1", "")
                            .queryParam("fid_input_price_2", "")
                            .queryParam("fid_vol_cnt",       "")
                            .queryParam("fid_trgt_cls_code",      "0")
                            .queryParam("fid_trgt_exls_cls_code", "0")
                            .queryParam("fid_div_cls_code",       "0")
                            .queryParam("fid_rsfl_rate1", "")
                            .queryParam("fid_rsfl_rate2", "")
                            .build())
                    .header("authorization", "Bearer " + token)
                    .header("appkey",    appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id",     "FHPST01700000")
                    .header("custtype",  "P")
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            res -> res.bodyToMono(String.class)
                                    .map(body -> new KisApiException("등락률순위 조회 실패: " + body, res.statusCode().value())))
                    .bodyToMono(KisChangeRateRankResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || !response.isSuccess()) {
                log.warn("KIS 등락률순위 비정상: {}", response != null ? response.message() : "null");
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (Exception e) {
            log.error("등락률순위 조회 중 예외: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 현재가 Raw Output 반환 (StockDetailResponse 구성용)
     */
    public Optional<KisPriceResponse.Output> getCurrentPriceRaw(String ticker) {
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
                                    .map(body -> new KisApiException("현재가(Raw) 조회 실패 [" + ticker + "]: " + body, res.statusCode().value())))
                    .bodyToMono(KisPriceResponse.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || !response.isSuccess() || response.output() == null) {
                log.warn("KIS 현재가(Raw) 비정상 [{}]: {}", ticker, response != null ? response.message() : "null");
                return Optional.empty();
            }
            return Optional.of(response.output());
        } catch (Exception e) {
            log.error("현재가(Raw) 조회 중 예외 [{}]: {}", ticker, e.getMessage());
            return Optional.empty();
        }
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
