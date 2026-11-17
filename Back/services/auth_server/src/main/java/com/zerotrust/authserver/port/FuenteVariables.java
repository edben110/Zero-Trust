package com.zerotrust.authserver.port;

import java.util.Optional;

public interface FuenteVariables {

    Optional<String> valor(String nombre);
}
