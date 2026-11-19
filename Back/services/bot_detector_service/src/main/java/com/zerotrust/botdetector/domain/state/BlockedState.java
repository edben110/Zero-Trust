package com.zerotrust.botdetector.domain.state;

final class BlockedState implements IpRiskState {

    @Override
    public void onVoteReceived(IpTrafficContext context) {
        context.requestAlert();
    }

    @Override
    public String name() {
        return "BLOQUEADA";
    }

    @Override
    public boolean esFinal() {
        return true;
    }
}
