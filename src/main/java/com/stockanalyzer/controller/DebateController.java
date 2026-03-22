package com.stockanalyzer.controller;

import com.stockanalyzer.dto.request.DebateRequest;
import com.stockanalyzer.service.DebateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "토론", description = "AI 에이전트 주식 토론")
@RestController
@RequestMapping("/api/debate")
public class DebateController {

    private final DebateService debateService;

    public DebateController(DebateService debateService) {
        this.debateService = debateService;
    }

    @Operation(
            summary = "AI 주식 토론",
            description = "종목명 2개 이상을 입력하면 AI 에이전트가 4라운드 토론을 진행하고 SSE로 실시간 스트리밍합니다."
    )
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter debate(@RequestBody DebateRequest request) {
        return debateService.startDebate(request.stocks());
    }
}
