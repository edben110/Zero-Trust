package com.zerotrust.authserver.adapter.config;

import com.zerotrust.authserver.port.FuenteVariables;

import java.util.Optional;

public class FuenteVariablesEntorno implements FuenteVariables {

    @Override
    public Optional<String> valor(String nombre) {
        return Optional.ofNullable(System.getenv(nombre));
    }
}
