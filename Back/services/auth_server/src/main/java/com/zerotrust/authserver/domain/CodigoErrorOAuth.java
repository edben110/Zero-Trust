package com.zerotrust.authserver.domain;

public enum CodigoErrorOAuth {
    INVALID_REQUEST("invalid_request"),
    INVALID_CLIENT("invalid_client"),
    INVALID_SCOPE("invalid_scope"),
    UNSUPPORTED_GRANT_TYPE("unsupported_grant_type");

    private final String texto;

    CodigoErrorOAuth(String texto) {
        this.texto = texto;
    }

    public String texto() {
        return texto;
    }
}
