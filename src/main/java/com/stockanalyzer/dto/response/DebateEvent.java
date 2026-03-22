package com.stockanalyzer.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DebateEvent(
        String type,        // "message" | "conclusion" | "done" | "error"
        String agentName,
        String agentRole,   // "advocate" | "critic"
        String targetStock,
        Integer round,
        String roundName,
        String message,
        List<ScoreDto> scores,
        String summary
) {
    public record ScoreDto(String name, String ticker, int score, String reason) {}

    public static DebateEvent message(String agentName, String agentRole, String targetStock,
                                      int round, String roundName, String message) {
        return new DebateEvent("message", agentName, agentRole, targetStock, round, roundName, message, null, null);
    }

    public static DebateEvent conclusion(List<ScoreDto> scores, String summary) {
        return new DebateEvent("conclusion", null, null, null, null, null, null, scores, summary);
    }

    public static DebateEvent done() {
        return new DebateEvent("done", null, null, null, null, null, null, null, null);
    }

    public static DebateEvent loading(String msg) {
        return new DebateEvent("loading", null, null, null, null, null, msg, null, null);
    }

    public static DebateEvent error(String msg) {
        return new DebateEvent("error", null, null, null, null, null, msg, null, null);
    }
}
