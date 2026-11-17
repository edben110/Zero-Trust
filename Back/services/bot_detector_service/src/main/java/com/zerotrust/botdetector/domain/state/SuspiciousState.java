package com.zerotrust.botdetector.domain.state;

import com.zerotrust.botdetector.domain.model.IpTrafficContext;

/**
 * Estado de sospecha. La IP ya superó el umbral normal.
 * Transiciona a BLOQUEADA si supera el umbral crítico.
 * Genera una alerta en cada voto adicional recibido en este estado.
 */
public final class SuspiciousState implements IpRiskState {

    @Override
    public void onVoteReceived(IpTrafficContext context) {
        context.incrementVoteCount();
        context.requestAlert();   // avisa al servicio que debe emitir una alerta

        if (context.voteCount() > context.blockedThreshold()) {
            context.transitionTo(new BlockedState());
        }
    }

    @Override
    public String name() {
        return "SOSPECHOSA";
    }
}
