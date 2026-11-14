package com.zerotrust.voterregistration.config;

// Tarea única: el proceso informa con su código de salida si el padrón quedó registrado.
public final class CodigoSalida {

    private CodigoSalida() {
    }

    public static int de(Runnable tarea) {
        try {
            tarea.run();
            return 0;
        } catch (RuntimeException e) {
            return 1;
        }
    }
}
