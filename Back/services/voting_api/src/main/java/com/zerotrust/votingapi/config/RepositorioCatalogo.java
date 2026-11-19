package com.zerotrust.votingapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.domain.Candidato;
import com.zerotrust.votingapi.domain.CandidateId;
import com.zerotrust.votingapi.domain.Catalogo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Fuente única de verdad para lectura de votos y edición del catálogo. */
public final class RepositorioCatalogo {
    private final Path archivo;
    private final ObjectMapper json;
    private volatile Catalogo actual;

    public RepositorioCatalogo(Path inicial, Path archivo, ObjectMapper json) {
        this.archivo = archivo;
        this.json = json;
        try {
            if (Files.notExists(archivo)) {
                Files.createDirectories(archivo.getParent());
                Files.copy(inicial, archivo);
            }
            this.actual = new LectorCatalogo().interpretar(Files.readAllBytes(archivo));
        } catch (IOException e) {
            throw new IllegalStateException("CATALOG_STATE_FILE: no se puede iniciar el catálogo", e);
        }
    }

    public Catalogo actual() {
        return actual;
    }

    public synchronized Catalogo crear(String nombre, String lista) {
        Candidato nuevo = new Candidato(new CandidateId(UUID.randomUUID().toString()),
                validar(nombre), validar(lista));
        List<Candidato> candidatos = new ArrayList<>(actual.candidatos());
        candidatos.add(nuevo);
        return guardar(new Catalogo(candidatos, actual.regiones()));
    }

    public synchronized Catalogo editar(String id, String nombre, String lista) {
        CandidateId buscado = new CandidateId(id);
        List<Candidato> candidatos = new ArrayList<>(actual.candidatos());
        int indice = indice(candidatos, buscado);
        candidatos.set(indice, new Candidato(buscado, validar(nombre), validar(lista)));
        return guardar(new Catalogo(candidatos, actual.regiones()));
    }

    public synchronized Catalogo eliminar(String id) {
        CandidateId buscado = new CandidateId(id);
        List<Candidato> candidatos = new ArrayList<>(actual.candidatos());
        int indice = indice(candidatos, buscado);
        if (candidatos.size() <= 2) {
            throw new IllegalArgumentException("La votación debe conservar al menos dos candidatos.");
        }
        candidatos.remove(indice);
        return guardar(new Catalogo(candidatos, actual.regiones()));
    }

    private static int indice(List<Candidato> candidatos, CandidateId id) {
        for (int i = 0; i < candidatos.size(); i++) {
            if (candidatos.get(i).candidateId().equals(id)) return i;
        }
        throw new IllegalArgumentException("Candidato no encontrado.");
    }

    private static String validar(String texto) {
        if (texto == null || texto.isBlank() || texto.length() > 120) {
            throw new IllegalArgumentException("Nombre y lista deben tener entre 1 y 120 caracteres.");
        }
        return texto.trim();
    }

    private Catalogo guardar(Catalogo siguiente) {
        Path temporal = null;
        try {
            temporal = Files.createTempFile(archivo.getParent(), "catalogo-", ".json");
            List<Map<String, String>> candidatos = siguiente.candidatos().stream()
                    .map(c -> Map.of("candidateId", c.candidateId().valor(),
                            "name", c.nombre(), "list", c.lista())).toList();
            json.writeValue(temporal.toFile(), Map.of("candidates", candidatos,
                    "regions", siguiente.regiones().stream().map(r -> r.valor()).toList()));
            Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            actual = siguiente;
            return siguiente;
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible guardar el catálogo.", e);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); } catch (IOException ignored) { }
            }
        }
    }
}
