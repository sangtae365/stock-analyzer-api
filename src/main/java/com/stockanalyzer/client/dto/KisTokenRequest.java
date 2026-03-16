package com.stockanalyzer.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisTokenRequest(
    @JsonProperty("grant_type")  String grantType,
    @JsonProperty("appkey")      String appkey,
    @JsonProperty("appsecret")   String appsecret
) {
    public static KisTokenRequest of(String appkey, String appsecret) {
        return new KisTokenRequest("client_credentials", appkey, appsecret);
    }
}
