package com.zerotrust.uservalidation.domain;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Política de elegibilidad: un votante está habilitado si eligible_voters lo publicó y no lo ha retirado.
public final class PadronElectoral {

    // El consumidor de Kafka escribe mientras los listeners RPC leen desde otros hilos.
    private final Set<VoterId> habilitados = ConcurrentHashMap.newKeySet();

    public void habilitar(VoterId voterId) {
        habilitados.add(voterId);
    }

    public void retirar(VoterId voterId) {
        habilitados.remove(voterId);
    }

    public boolean estaHabilitado(VoterId voterId) {
        return habilitados.contains(voterId);
    }

    public int total() {
        return habilitados.size();
    }
}
