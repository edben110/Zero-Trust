package com.zerotrust.voterregistration.adapter.archivo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.voterregistration.domain.Cedula;
import com.zerotrust.voterregistration.domain.ElectionId;
import com.zerotrust.voterregistration.exception.PadronInvalidoException;
import com.zerotrust.voterregistration.domain.RegistroVotanteCrudo;
import com.zerotrust.voterregistration.port.DerivadorVoterId;

// El archivo trae la cédula de cada votante; aquí mismo se convierte en el UUID derivado (el mismo que devuelve
// POST /voter-id de voting_api). La cédula no sale de este método: ni PostgreSQL ni Kafka la reciben.
public class LectorArchivoVotantes {

    private final ObjectMapper json = new ObjectMapper();
    private final DerivadorVoterId derivador;
    private final ElectionId eleccion;

    public LectorArchivoVotantes(DerivadorVoterId derivador, ElectionId eleccion) {
        this.derivador = derivador;
        this.eleccion = eleccion;
    }

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
            crudos.add(new RegistroVotanteCrudo(voterIdDe(texto(nodo, "cedula"), i), texto(nodo, "region")));
        }
        return crudos;
    }

    // El mensaje de error no incluye el valor de la cédula.
    private String voterIdDe(String cedula, int posicion) {
        try {
            return derivador.derivar(new Cedula(cedula), eleccion).valor();
        } catch (IllegalArgumentException e) {
            throw new PadronInvalidoException("registro [" + posicion + "]: cedula inválida");
        }
    }

    private static String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor != null && valor.isTextual() ? valor.textValue() : null;
    }
}
