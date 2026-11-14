package com.zerotrust.authserver.adapter.config;

import com.zerotrust.authserver.domain.ConfiguracionInvalidaException;
import com.zerotrust.authserver.port.LectorArchivos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class LectorArchivosSistema implements LectorArchivos {

    @Override
    public byte[] leer(String ruta) {
        try {
            return Files.readAllBytes(Path.of(ruta));
        } catch (IOException | InvalidPathException e) {
            throw new ConfiguracionInvalidaException("no se puede leer el archivo");
        }
    }
}
