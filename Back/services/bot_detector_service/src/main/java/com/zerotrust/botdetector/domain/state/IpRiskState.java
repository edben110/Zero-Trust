package com.zerotrust.botdetector.domain.state;

interface IpRiskState {

    void onVoteReceived(IpTrafficContext context);

    String name();

    // BLOQUEADA no se reinicia con la ventana: solo una intervención administrativa la devuelve a NORMAL.
    default boolean esFinal() {
        return false;
    }
}
