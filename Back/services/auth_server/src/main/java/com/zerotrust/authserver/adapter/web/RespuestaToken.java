package com.zerotrust.authserver.adapter.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RespuestaToken(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") int expiresIn) {
}
