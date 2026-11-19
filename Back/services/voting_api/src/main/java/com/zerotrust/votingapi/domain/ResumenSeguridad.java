package com.zerotrust.votingapi.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ResumenSeguridad(long incidentes, long reales, long simulacros, long duplicados,
                               long regionIncorrecta, long ipsConIncidentes, Map<String, Long> riesgoActual) {

    public ResumenSeguridad {
        if (riesgoActual == null) {
            throw new IllegalArgumentException("riesgoActual no puede ser nulo");
        }
        // LinkedHashMap: el panel muestra los niveles en orden NORMAL, SOSPECHOSA, BLOQUEADA.
        riesgoActual = Collections.unmodifiableMap(new LinkedHashMap<>(riesgoActual));
    }
}
