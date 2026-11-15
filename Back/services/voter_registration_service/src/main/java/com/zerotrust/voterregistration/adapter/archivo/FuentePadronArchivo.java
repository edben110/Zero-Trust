package com.zerotrust.voterregistration.adapter.archivo;

import com.zerotrust.voterregistration.domain.PadronInvalidoException;
import com.zerotrust.voterregistration.domain.RegistroVotanteCrudo;
import com.zerotrust.voterregistration.port.FuentePadron;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FuentePadronArchivo implements FuentePadron {

    private final Path ruta;
    private final LectorArchivoVotantes lector;

    public FuentePadronArchivo(Path ruta, LectorArchivoVotantes lector) {
        this.ruta = ruta;
        this.lector = lector;
    }

    @Override
    public List<RegistroVotanteCrudo> leer() {
        try {
            return lector.interpretar(Files.readAllBytes(ruta));
        } catch (IOException e) {
            throw new PadronInvalidoException("no se puede leer el archivo indicado");
        }
    }
}
