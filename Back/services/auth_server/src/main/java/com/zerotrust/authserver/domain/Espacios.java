package com.zerotrust.authserver.domain;

final class Espacios {

    private Espacios() {
    }

    static boolean contieneEspacioEnBlanco(String texto) {
        return texto.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }
}
