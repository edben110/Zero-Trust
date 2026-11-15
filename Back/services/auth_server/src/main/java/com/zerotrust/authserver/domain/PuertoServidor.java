package com.zerotrust.authserver.domain;

public record PuertoServidor(int valor) {

    private static final int MINIMO = 1024;
    private static final int MAXIMO = 65535;
    private static final int POR_DEFECTO = 8443;

    public PuertoServidor {
        if (valor < MINIMO || valor > MAXIMO) {
            throw new IllegalArgumentException("SERVER_PORT debe estar entre 1024 y 65535");
        }
    }

    public static PuertoServidor desde(String texto) {
        return new PuertoServidor(EnteroEstricto.desde(texto, "SERVER_PORT"));
    }

    public static PuertoServidor porDefecto() {
        return new PuertoServidor(POR_DEFECTO);
    }
}
