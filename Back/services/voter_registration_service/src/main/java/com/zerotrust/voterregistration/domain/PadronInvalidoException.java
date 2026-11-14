package com.zerotrust.voterregistration.domain;

public class PadronInvalidoException extends RuntimeException {

    public PadronInvalidoException(String mensaje) {
        super("VOTERS_FILE: " + mensaje, null, false, false);
    }
}
