package com.zerotrust.authserver.domain;

import java.net.URI;
import java.net.URISyntaxException;

public record Emisor(String valor) {

    public Emisor {
        if (valor == null || valor.isBlank() || !esUrlHttpsConHost(valor)) {
            throw new IllegalArgumentException("AUTH_ISSUER debe ser una URL absoluta https con host");
        }
    }

    private static boolean esUrlHttpsConHost(String texto) {
        try {
            URI uri = new URI(texto);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && !uri.getHost().isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
