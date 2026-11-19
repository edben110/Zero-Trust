package com.zerotrust.regionalrollup.domain.exception;

/**
 * No se pudo publicar un resultado tras agotar los intentos.
 */
public class PublicacionFallida extends RuntimeException {

    public PublicacionFallida(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
