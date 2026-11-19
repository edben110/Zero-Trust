package com.zerotrust.voterregistration.exception;

public class PadronInvalidoException extends RuntimeException {

    public PadronInvalidoException(String mensaje) {
        super("VOTERS_FILE: " + mensaje, null, false, false);
    }
}
