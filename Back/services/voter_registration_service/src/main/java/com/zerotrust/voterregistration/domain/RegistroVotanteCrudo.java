package com.zerotrust.voterregistration.domain;

// Datos tal como vienen del archivo; PadronInicial decide si son válidos.
public record RegistroVotanteCrudo(String voterId, String region) {

    @Override
    public String toString() {
        return "RegistroVotanteCrudo[voterId=" + voterId + "]";
    }
}
