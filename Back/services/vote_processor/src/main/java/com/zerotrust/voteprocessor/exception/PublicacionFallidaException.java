package com.zerotrust.voteprocessor.exception;

public class PublicacionFallidaException extends RuntimeException {

    public PublicacionFallidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
