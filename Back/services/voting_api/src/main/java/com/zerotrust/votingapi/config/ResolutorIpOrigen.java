package com.zerotrust.votingapi.config;

import com.zerotrust.votingapi.domain.SourceIp;

import java.util.Locale;

// X-Forwarded-For solo vale detrás de un proxy de confianza: si no, un cliente falsificaría su IP ante bot_detector.
public final class ResolutorIpOrigen {

    private final boolean confiarEnCabeceras;

    private ResolutorIpOrigen(boolean confiarEnCabeceras) {
        this.confiarEnCabeceras = confiarEnCabeceras;
    }

    public static ResolutorIpOrigen desde(String trustForwardedHeaders) {
        if (trustForwardedHeaders == null || trustForwardedHeaders.isBlank()) {
            return new ResolutorIpOrigen(false);
        }
        String valor = trustForwardedHeaders.strip().toLowerCase(Locale.ROOT);
        if (!valor.equals("true") && !valor.equals("false")) {
            throw new IllegalStateException("TRUST_FORWARDED_HEADERS debe ser true o false");
        }
        return new ResolutorIpOrigen(valor.equals("true"));
    }

    public SourceIp resolver(String direccionRemota, String xForwardedFor) {
        if (confiarEnCabeceras && xForwardedFor != null) {
            String primera = xForwardedFor.split(",", 2)[0].strip();
            if (!primera.isEmpty() && primera.length() <= 45) {
                return new SourceIp(primera);
            }
        }
        return new SourceIp(direccionRemota);
    }
}
