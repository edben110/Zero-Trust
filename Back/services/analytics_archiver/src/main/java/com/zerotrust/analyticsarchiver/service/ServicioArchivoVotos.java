package com.zerotrust.analyticsarchiver.service;

import com.zerotrust.analyticsarchiver.domain.VotoAuditado;
import com.zerotrust.analyticsarchiver.port.RepositorioAuditoria;

public class ServicioArchivoVotos {

    private final RepositorioAuditoria repositorio;

    public ServicioArchivoVotos(RepositorioAuditoria repositorio) {
        this.repositorio = repositorio;
    }

    public boolean archivar(VotoAuditado voto) {
        return repositorio.guardarSiNoExiste(voto);
    }
}
