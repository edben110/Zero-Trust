package com.zerotrust.authserver.domain;

import java.util.LinkedHashSet;
import java.util.Set;

public record SolicitudScopes(boolean todos, Set<Scope> pedidos) {

    public SolicitudScopes {
        pedidos = Set.copyOf(pedidos);
    }

    // Solo se llama después de autenticar: un scope mal formado es invalid_scope, no invalid_request (RF-24).
    public static SolicitudScopes desde(String crudo) {
        if (crudo == null || crudo.chars().allMatch(c -> c == ' ')) {
            return new SolicitudScopes(true, Set.of());
        }
        Set<Scope> pedidos = new LinkedHashSet<>();
        for (String parte : crudo.strip().split(" +")) {
            pedidos.add(scopeOInvalidScope(parte));
        }
        return new SolicitudScopes(false, pedidos);
    }

    private static Scope scopeOInvalidScope(String parte) {
        try {
            return new Scope(parte);
        } catch (IllegalArgumentException e) {
            throw new ScopeNoPermitidoException();
        }
    }
}
