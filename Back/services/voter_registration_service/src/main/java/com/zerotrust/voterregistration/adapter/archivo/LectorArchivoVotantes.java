package com.zerotrust.voterregistration.adapter.archivo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.voterregistration.domain.PadronInvalidoException;
import com.zerotrust.voterregistration.domain.RegistroVotanteCrudo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LectorArchivoVotantes {

    private final ObjectMapper json = new ObjectMapper();

    public List<RegistroVotanteCrudo> interpretar(byte[] contenido) {
        JsonNode raiz;
        try {
            raiz = json.readTree(contenido);
        } catch (IOException e) {
            throw new PadronInvalidoException("no es JSON válido");
        }
        if (raiz == null || !raiz.isArray()) {
            throw new PadronInvalidoException("debe ser una lista de votantes");
        }
        List<RegistroVotanteCrudo> crudos = new ArrayList<>();
        for (int i = 0; i < raiz.size(); i++) {
            JsonNode nodo = raiz.get(i);
            if (!nodo.isObject()) {
                throw new PadronInvalidoException("registro [" + i + "]: debe ser un objeto");
            }
            crudos.add(new RegistroVotanteCrudo(texto(nodo, "voterId"), texto(nodo, "fullName"), texto(nodo, "region")));
        }
        return crudos;
    }

    private static String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor != null && valor.isTextual() ? valor.textValue() : null;
    }
}
