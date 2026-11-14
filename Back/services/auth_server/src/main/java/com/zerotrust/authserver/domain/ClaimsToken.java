package com.zerotrust.authserver.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record ClaimsToken(String iss, String sub, List<Audiencia> aud, Optional<String> scope, long iat, long exp) {

    public ClaimsToken {
        aud = List.copyOf(aud);
    }

    public static ClaimsToken para(Emisor emisor, ClienteRegistrado cliente, ScopesConcedidos scopes,
                                   Instant ahora, Ttl ttl) {
        long iat = ahora.getEpochSecond();
        return new ClaimsToken(emisor.valor(), cliente.clientId().valor(), cliente.audiencias(),
                scopes.comoClaim(), iat, iat + ttl.segundos());
    }
}
