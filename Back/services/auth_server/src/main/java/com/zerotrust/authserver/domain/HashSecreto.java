package com.zerotrust.authserver.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record HashSecreto(String valor) {

    private static final Pattern FORMATO = Pattern.compile("^\\$2[aby]\\$(\\d{2})\\$[./A-Za-z0-9]{53}$");
    private static final int COSTE_MINIMO = 10;
    private static final int COSTE_MAXIMO = 14;

    public HashSecreto {
        if (valor == null) {
            throw new IllegalArgumentException("secret_hash no puede ser nulo");
        }
        Matcher matcher = FORMATO.matcher(valor);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("secret_hash no tiene el formato BCrypt");
        }
        int coste = Integer.parseInt(matcher.group(1));
        if (coste < COSTE_MINIMO || coste > COSTE_MAXIMO) {
            throw new IllegalArgumentException("el coste BCrypt debe estar entre 10 y 14");
        }
    }

    public int coste() {
        return Integer.parseInt(valor.substring(4, 6));
    }

    @Override
    public String toString() {
        return "HashSecreto[coste=" + coste() + "]";
    }
}
