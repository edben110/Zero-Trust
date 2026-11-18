package com.zerotrust.voterregistration.domain;

import java.text.Normalizer;
import java.util.regex.Pattern;

// Número de cédula ya normalizado. Se conserva como texto para no perder los ceros iniciales.
// Solo vive en memoria durante la solicitud: toString no revela el valor y los mensajes de error tampoco.
public record Cedula(String valor) {

    private static final Pattern FORMATO = Pattern.compile("[0-9]{6,10}");

    // Normalización única: NFKC (dígitos de ancho completo y espacios especiales pasan a ASCII),
    // se descartan espacios y puntos separadores y quedan de 6 a 10 dígitos ASCII.
    public Cedula {
        if (valor == null) {
            throw new IllegalArgumentException("cedula no puede ser nula");
        }
        String compatible = Normalizer.normalize(valor, Normalizer.Form.NFKC);
        StringBuilder limpio = new StringBuilder(compatible.length());
        for (int i = 0; i < compatible.length(); i++) {
            char c = compatible.charAt(i);
            if (c != '.' && !Character.isWhitespace(c) && !Character.isSpaceChar(c)) {
                limpio.append(c);
            }
        }
        if (!FORMATO.matcher(limpio).matches()) {
            throw new IllegalArgumentException("cedula con formato inválido");
        }
        valor = limpio.toString();
    }

    @Override
    public String toString() {
        return "Cedula[***]";
    }
}
