package com.stockanalyzer.client;

import com.stockanalyzer.client.dto.KisPriceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KIS OpenAPI 실제 연동 통합 테스트.
 *
 * 실행 조건:
 *   - application-local.yml 에 유효한 kis.app-key / kis.app-secret 이 설정되어 있어야 합니다.
 *   - 한국 주식 시장 운영 시간(평일 09:00~15:30) 외에도 호출 가능하나,
 *     장 마감 후에는 전일 종가 기준 데이터가 반환됩니다.
 *
 * 실행 방법:
 *   mvn test -Dtest=StockMarketClientIntegrationTest -Dspring.profiles.active=local -pl backend
 */
@SpringBootTest
@ActiveProfiles("local")
class StockMarketClientIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(StockMarketClientIntegrationTest.class);

    @Autowired
    private StockMarketClient stockMarketClient;

    private static final String TICKER_KAKAO_PAY   = "377300";
    private static final String TICKER_KAKAO_GAMES = "293490";
    private static final String TICKER_KAKAO_BANK  = "323410";
    private static final String TICKER_IMA         = "101060";

    @Test
    @DisplayName("카카오페이(377300) 현재가 조회")
    void kakaoPay_currentPrice() {
        assertStock(TICKER_KAKAO_PAY);
    }

    @Test
    @DisplayName("카카오게임즈(293490) 현재가 조회")
    void kakaoGames_currentPrice() {
        assertStock(TICKER_KAKAO_GAMES);
    }

    @Test
    @DisplayName("카카오뱅크(323410) 현재가 조회")
    void kakaoBank_currentPrice() {
        assertStock(TICKER_KAKAO_BANK);
    }

    @Test
    @DisplayName("IMA(101060) 현재가 조회")
    void ima_currentPrice() {
        assertStock(TICKER_IMA);
    }

    private void assertStock(String ticker) {
        log.info("━━━ [{}] 조회 시작 ━━━", ticker);

        Optional<KisPriceResponse.Output> result = stockMarketClient.getCurrentPriceRaw(ticker);

        assertThat(result)
                .as("[%s] KIS API 응답이 비어 있습니다.", ticker)
                .isPresent();

        KisPriceResponse.Output o = result.get();

        log.info("  종목코드  : {}", o.ticker());
        log.info("  종목명    : {}", o.htsName());
        log.info("  현재가    : {}원", o.currentPrice());
        log.info("  등락률    : {}%", o.changeRate());
        log.info("  시가총액  : {}억원", o.marketCapBil());

        assertThat(o.ticker()).isNotBlank();
        assertThat(o.currentPrice()).isNotBlank();

        log.info("  ✔ [{}] 검증 통과", ticker);
    }
}
