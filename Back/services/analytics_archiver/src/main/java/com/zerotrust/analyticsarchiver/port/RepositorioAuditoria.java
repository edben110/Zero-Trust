package com.zerotrust.analyticsarchiver.port;

import com.zerotrust.analyticsarchiver.domain.VotoAuditado;

public interface RepositorioAuditoria {

    // Devuelve false si el voto ya estaba guardado (idempotencia por vote_id).
    boolean guardarSiNoExiste(VotoAuditado voto);
}
