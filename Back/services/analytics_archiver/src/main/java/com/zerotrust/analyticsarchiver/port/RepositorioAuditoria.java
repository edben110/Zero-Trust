package com.zerotrust.analyticsarchiver.port;

import com.zerotrust.analyticsarchiver.domain.VotoAuditado;

public interface RepositorioAuditoria {

    boolean guardarSiNoExiste(VotoAuditado voto);
}
