package com.zerotrust.votingapi.port;

import java.util.function.Supplier;

public interface EjecutorTransaccional {

    // Confirma si la acción termina; si lanza una excepción, revierte todo lo hecho dentro de ella.
    <T> T ejecutar(Supplier<T> accion);
}
