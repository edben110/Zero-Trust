package com.zerotrust.botdetector.domain.state;

import com.zerotrust.botdetector.domain.model.IpTrafficContext;

/**
 * Estado final. La IP está bloqueada.
 * Cada voto recibido en este estado genera una alerta de alta severidad.
 * No hay transición posterior: la IP permanece bloqueada hasta intervención manual.
 */
public final class BlockedState implements IpRiskState {

    @Override
    public void onVoteReceived(IpTrafficContext context) {
        // La IP sigue intentando votar estando bloqueada → alerta crítica
        context.requestAlert();
    }

    @Override
    public String name() {
        return "BLOQUEADA";
    }
}
