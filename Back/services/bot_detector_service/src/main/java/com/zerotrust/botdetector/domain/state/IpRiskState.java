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

    /**
     * Indica si el estado es terminal. BLOQUEADA no se reinicia con la ventana
     * de tiempo: solo una intervención administrativa devuelve la IP a NORMAL.
     */
    default boolean esFinal() {
        return false;
    }
}
