package com.zerotrust.uservalidation.adapter.kafka;

import com.zerotrust.uservalidation.domain.VoterId;
import com.zerotrust.uservalidation.service.ServicioPadronElectoral;

public sealed interface CambioPadron {

    void aplicarEn(ServicioPadronElectoral servicio);

    record Alta(VoterId voterId) implements CambioPadron {
        @Override
        public void aplicarEn(ServicioPadronElectoral servicio) {
            servicio.registrarElegible(voterId);
        }
    }

    record Baja(VoterId voterId) implements CambioPadron {
        @Override
        public void aplicarEn(ServicioPadronElectoral servicio) {
            servicio.retirar(voterId);
        }
    }
}
