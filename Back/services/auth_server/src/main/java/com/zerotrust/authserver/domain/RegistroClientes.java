package com.zerotrust.authserver.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RegistroClientes {

    private final Map<ClientId, ClienteRegistrado> clientes;
    private final int costeComun;

    public RegistroClientes(List<ClienteRegistrado> lista) {
        if (lista == null || lista.isEmpty()) {
            throw new IllegalArgumentException("clients no puede estar vacía");
        }
        Map<ClientId, ClienteRegistrado> porId = new LinkedHashMap<>();
        for (ClienteRegistrado cliente : lista) {
            if (porId.putIfAbsent(cliente.clientId(), cliente) != null) {
                throw new IllegalArgumentException("client_id repetido");
            }
        }
        int coste = lista.get(0).hash().coste();
        if (lista.stream().anyMatch(cliente -> cliente.hash().coste() != coste)) {
            throw new IllegalArgumentException("todos los secret_hash deben tener el mismo coste");
        }
        this.clientes = Map.copyOf(porId);
        this.costeComun = coste;
    }

    public Optional<ClienteRegistrado> buscar(ClientId clientId) {
        return Optional.ofNullable(clientes.get(clientId));
    }

    public int costeComun() {
        return costeComun;
    }
}
