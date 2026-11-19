package com.zerotrust.botdetector.domain.state;

// La alerta sale en el mismo voto que cruza el umbral: la transición ya es el primer indicio.
final class NormalState implements IpRiskState {

    @Override
    public void onVoteReceived(IpTrafficContext context) {
        context.incrementVoteCount();
        if (context.voteCount() > context.suspiciousThreshold()) {
            context.transitionTo(new SuspiciousState());
            context.requestAlert();
        }
    }

    @Override
    public String name() {
        return "NORMAL";
    }
}
