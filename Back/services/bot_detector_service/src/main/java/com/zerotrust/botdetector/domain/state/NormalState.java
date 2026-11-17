package com.zerotrust.botdetector.domain.state;

import com.zerotrust.botdetector.domain.model.IpTrafficContext;

/**
 * Estado inicial. La IP se comporta con normalidad.
 * Transiciona a SOSPECHOSA si supera el umbral de votos por ventana de tiempo.
 */
public final class NormalState implements IpRiskState {

    @Override
    public void onVoteReceived(IpTrafficContext context) {
        context.incrementVoteCount();
        if (context.voteCount() > context.suspiciousThreshold()) {
            context.transitionTo(new SuspiciousState());
        }
    }

    @Override
    public String name() {
        return "NORMAL";
    }
}
