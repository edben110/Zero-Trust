package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.FraudKind;
import com.zerotrust.votingapi.domain.SourceIp;

// Best-effort: un fallo al notificar nunca cambia la respuesta que recibe el votante.
public interface RegistroIncidentesFraude {

    void registrar(FraudKind tipo, SourceIp ip);
}
