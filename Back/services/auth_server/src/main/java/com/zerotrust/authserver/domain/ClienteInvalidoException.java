package com.zerotrust.authserver.domain;

public class ClienteInvalidoException extends ErrorOAuthException {

    private final boolean porBasic;

    public ClienteInvalidoException(boolean porBasic, String clientIdCrudo) {
        super(CodigoErrorOAuth.INVALID_CLIENT, clientIdCrudo);
        this.porBasic = porBasic;
    }

    public boolean porBasic() {
        return porBasic;
    }
}
