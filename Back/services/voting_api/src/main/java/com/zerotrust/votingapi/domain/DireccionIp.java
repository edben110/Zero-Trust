package com.zerotrust.votingapi.domain;

import java.util.regex.Pattern;

/**
 * Value Object para una IP observada en una prueba de seguridad.
 *
 * No resuelve nombres DNS (evita SSRF y bloqueos): solo acepta literales IPv4
 * o IPv6. La validación es Fail-Fast y el valor queda inmutable.
 */
public record DireccionIp(String valor) {

    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    private static final int LARGO_MAXIMO = 45;

    public DireccionIp {
        if (valor == null || valor.isBlank()) {
            throw new SolicitudSeguridadInvalidaException("La IP es obligatoria.");
        }
        String normalizada = valor.strip();
        if (normalizada.length() > LARGO_MAXIMO) {
            throw new SolicitudSeguridadInvalidaException("La IP supera 45 caracteres.");
        }
        if (!IPV4.matcher(normalizada).matches() && !esIpv6(normalizada)) {
            throw new SolicitudSeguridadInvalidaException("La IP no es un literal IPv4 o IPv6 válido.");
        }
        valor = normalizada;
    }

    private static boolean esIpv6(String texto) {
        if (texto.indexOf(':') < 0 || !texto.matches("[0-9a-fA-F:.]+") || texto.contains(":::")) {
            return false;
        }
        long dosPuntos = texto.chars().filter(c -> c == ':').count();
        return dosPuntos >= 2 && dosPuntos <= 8;
    }
}
