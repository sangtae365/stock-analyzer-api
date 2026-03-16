package com.stockanalyzer.service;

import com.stockanalyzer.dto.response.PopularKeywordResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeywordService {

    public PopularKeywordResponse getPopularKeywords(int limit) {
        List<PopularKeywordResponse.KeywordDto> all = List.of(
            new PopularKeywordResponse.KeywordDto(1,  "AI",    3820),
            new PopularKeywordResponse.KeywordDto(2,  "반도체",  2910),
            new PopularKeywordResponse.KeywordDto(3,  "카카오",  1540),
            new PopularKeywordResponse.KeywordDto(4,  "핀테크",  1230),
            new PopularKeywordResponse.KeywordDto(5,  "바이오",   980),
            new PopularKeywordResponse.KeywordDto(6,  "게임",     870),
            new PopularKeywordResponse.KeywordDto(7,  "전기차",   760),
            new PopularKeywordResponse.KeywordDto(8,  "HBM",     650),
            new PopularKeywordResponse.KeywordDto(9,  "자동차",   540),
            new PopularKeywordResponse.KeywordDto(10, "금융",     430)
        );
        return new PopularKeywordResponse(all.stream().limit(limit).toList());
    }
}
