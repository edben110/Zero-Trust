package com.zerotrust.regionalrollup.exception;

/**
 * No se pudo publicar un resultado tras agotar los intentos.
 */
public class PublicacionFallidaException extends RuntimeException {

    public PublicacionFallidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
