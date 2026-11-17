package com.zerotrust.votingapi.adapter.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.domain.Candidato;
import com.zerotrust.votingapi.domain.CandidateId;
import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.domain.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class LectorCatalogo {

    private static final Set<String> CAMPOS_RAIZ = Set.of("candidates", "regions");
    private static final Set<String> CAMPOS_CANDIDATO = Set.of("candidateId", "name", "list");

    private final ObjectMapper json = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    public Catalogo interpretar(byte[] contenido) {
        JsonNode raiz = leer(contenido);
        exigirCampos(raiz, CAMPOS_RAIZ, "la raíz");
        List<Candidato> candidatos = new ArrayList<>();
        for (JsonNode nodo : lista(raiz, "candidates")) {
            exigirCampos(nodo, CAMPOS_CANDIDATO, "un candidato");
            candidatos.add(construir(() -> new Candidato(new CandidateId(texto(nodo, "candidateId")),
                    texto(nodo, "name"), texto(nodo, "list"))));
        }
        List<Region> regiones = new ArrayList<>();
        for (JsonNode nodo : lista(raiz, "regions")) {
            if (!nodo.isTextual()) {
                throw invalido("cada región debe ser texto");
            }
            regiones.add(construir(() -> new Region(nodo.textValue())));
        }
        return construir(() -> new Catalogo(candidatos, regiones));
    }

    private JsonNode leer(byte[] contenido) {
        try {
            JsonNode raiz = json.readTree(contenido);
            if (raiz == null || !raiz.isObject()) {
                throw invalido("la raíz debe ser un objeto");
            }
            return raiz;
        } catch (IOException e) {
            throw invalido("no es JSON válido");
        }
    }

    private static void exigirCampos(JsonNode nodo, Set<String> esperados, String donde) {
        if (!nodo.isObject() || nodo.size() != esperados.size()) {
            throw invalido(donde + " debe tener exactamente los campos " + esperados);
        }
        Iterator<String> campos = nodo.fieldNames();
        while (campos.hasNext()) {
            if (!esperados.contains(campos.next())) {
                throw invalido(donde + " tiene un campo desconocido");
            }
        }
    }

    private static JsonNode lista(JsonNode raiz, String campo) {
        JsonNode valor = raiz.get(campo);
        if (valor == null || !valor.isArray()) {
            throw invalido(campo + " debe ser una lista");
        }
        return valor;
    }

    private static String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor != null && valor.isTextual() ? valor.textValue() : null;
    }

    private static <T> T construir(java.util.function.Supplier<T> constructor) {
        try {
            return constructor.get();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw invalido(e.getMessage());
        }
    }

    private static IllegalStateException invalido(String detalle) {
        return new IllegalStateException("CATALOG_FILE: " + detalle);
    }
}
