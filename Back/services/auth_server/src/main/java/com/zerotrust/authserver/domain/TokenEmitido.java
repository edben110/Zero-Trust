package com.zerotrust.authserver.domain;

public record TokenEmitido(String accessToken, int expiresIn) {

    @Override
    public String toString() {
        return "TokenEmitido[expiresIn=" + expiresIn + "]";
    }
}
