package com.zerotrust.botdetector.domain.state;

final class SuspiciousState implements IpRiskState {

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
