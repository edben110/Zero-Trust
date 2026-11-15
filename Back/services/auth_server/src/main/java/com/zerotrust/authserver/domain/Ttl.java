package com.zerotrust.authserver.domain;

public record Ttl(int segundos) {

    private static final int MINIMO = 60;
    private static final int MAXIMO = 3600;
    private static final int POR_DEFECTO = 300;

    public Ttl {
        if (segundos < MINIMO || segundos > MAXIMO) {
            throw new IllegalArgumentException("AUTH_TOKEN_TTL debe estar entre 60 y 3600 segundos");
        }
    }

    public static Ttl desde(String texto) {
        return new Ttl(EnteroEstricto.desde(texto, "AUTH_TOKEN_TTL"));
    }

    public static Ttl porDefecto() {
        return new Ttl(POR_DEFECTO);
    }
}
