package com.zerotrust.uservalidation.port;

import com.zerotrust.uservalidation.domain.VoterId;

// La vista materializada de eligible_voters es un detalle de almacenamiento (hoy en memoria).
public interface PadronVotantes {

    void habilitar(VoterId voterId);

    void retirar(VoterId voterId);

    boolean estaHabilitado(VoterId voterId);
}
