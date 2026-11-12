package com.zerotrust.botdetector.domain.state;

import com.zerotrust.botdetector.domain.model.IpTrafficContext;

/**
 * Contrato del patrón State para el ciclo de riesgo de una IP.
 *
 * Cada estado encapsula su propia lógica de transición y de detección de
 * anomalías, eliminando la necesidad de bloques if-else o switch en el
 * contexto que los usa.
 */
public interface IpRiskState {

    /**
     * Evalúa un nuevo voto registrado desde esta IP.
     * Puede mutar el contexto para transicionar de estado.
     */
    void onVoteReceived(IpTrafficContext context);

    /**
     * Nombre canónico del estado (para serialización y alertas).
     */
    String name();
}
