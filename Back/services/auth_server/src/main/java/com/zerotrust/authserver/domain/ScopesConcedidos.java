package com.zerotrust.authserver.domain;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record ScopesConcedidos(List<Scope> scopes) {

    public ScopesConcedidos {
        scopes = List.copyOf(scopes);
    }

    public Optional<String> comoClaim() {
        if (scopes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(scopes.stream().map(Scope::valor).collect(Collectors.joining(" ")));
    }
}
