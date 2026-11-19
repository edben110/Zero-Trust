package com.zerotrust.authserver.domain;

import com.zerotrust.authserver.exception.ScopeNoPermitidoException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class ClienteRegistrado {

    private final ClientId clientId;
    private final HashSecreto hash;
    private final List<Scope> scopes;
    private final List<Audiencia> audiencias;

    public ClienteRegistrado(ClientId clientId, HashSecreto hash, List<Scope> scopes, List<Audiencia> audiencias) {
        this.clientId = Objects.requireNonNull(clientId, "client_id obligatorio");
        this.hash = Objects.requireNonNull(hash, "secret_hash obligatorio");
        this.scopes = List.copyOf(Objects.requireNonNull(scopes, "scopes obligatorio"));
        this.audiencias = List.copyOf(Objects.requireNonNull(audiencias, "audiences obligatorio"));
        if (new HashSet<>(this.scopes).size() != this.scopes.size()) {
            throw new IllegalArgumentException("scopes repetidos");
        }
        if (this.audiencias.isEmpty()) {
            throw new IllegalArgumentException("audiences no puede estar vacía");
        }
        if (new HashSet<>(this.audiencias).size() != this.audiencias.size()) {
            throw new IllegalArgumentException("audiences repetidas");
        }
        // Kafka autoriza por ACL sobre sub, así que un cliente solo de Kafka no necesita scopes.
        if (this.scopes.isEmpty() && !this.audiencias.equals(List.of(Audiencia.KAFKA))) {
            throw new IllegalArgumentException("scopes vacía solo se admite si audiences es exactamente [kafka]");
        }
    }

    public ScopesConcedidos concederScopes(SolicitudScopes solicitud) {
        if (solicitud.todos()) {
            return new ScopesConcedidos(scopes);
        }
        if (!scopes.containsAll(solicitud.pedidos())) {
            throw new ScopeNoPermitidoException();
        }
        return new ScopesConcedidos(scopes.stream().filter(solicitud.pedidos()::contains).toList());
    }

    public ClientId clientId() {
        return clientId;
    }

    public HashSecreto hash() {
        return hash;
    }

    public List<Audiencia> audiencias() {
        return audiencias;
    }
}
