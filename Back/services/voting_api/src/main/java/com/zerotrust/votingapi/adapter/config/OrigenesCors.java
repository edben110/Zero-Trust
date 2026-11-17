package com.zerotrust.votingapi.adapter.config;

import java.util.Arrays;
import java.util.List;

public record OrigenesCors(List<String> origenes) {

    public OrigenesCors {
        origenes = List.copyOf(origenes);
    }

    public static OrigenesCors desde(String corsAllowedOrigins) {
        if (corsAllowedOrigins == null) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS: variable obligatoria sin definir");
        }
        List<String> origenes = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::strip)
                .filter(origen -> !origen.isEmpty())
                .toList();
        if (origenes.isEmpty()) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS: debe tener al menos un origen");
        }
        for (String origen : origenes) {
            if (origen.contains("*") || !(origen.startsWith("https://") || origen.startsWith("http://"))) {
                throw new IllegalStateException("CORS_ALLOWED_ORIGINS: cada origen debe ser http(s)://host sin comodín");
            }
        }
        return new OrigenesCors(origenes);
    }
}
