package com.zerotrust.authserver.adapter.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.authserver.domain.Audiencia;
import com.zerotrust.authserver.domain.ClientId;
import com.zerotrust.authserver.domain.ClienteRegistrado;
import com.zerotrust.authserver.exception.ConfiguracionInvalidaException;
import com.zerotrust.authserver.domain.HashSecreto;
import com.zerotrust.authserver.domain.RegistroClientes;
import com.zerotrust.authserver.domain.Scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class InterpreteArchivoClientes {

    private static final String ORIGEN = "AUTH_CLIENTS_FILE";
    private static final Set<String> CAMPOS_CLIENTE = Set.of("client_id", "secret_hash", "scopes", "audiences");

    private final ObjectMapper mapper = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    public RegistroClientes interpretar(byte[] contenido) {
        JsonNode raiz = leerArbol(contenido);
        if (!raiz.isObject() || raiz.size() != 1 || !raiz.has("clients") || !raiz.get("clients").isArray()) {
            throw invalida("la raíz debe ser exactamente {\"clients\": [...]}");
        }
        JsonNode lista = raiz.get("clients");
        if (lista.isEmpty()) {
            throw invalida("clients no puede estar vacía");
        }
        List<ClienteRegistrado> clientes = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            clientes.add(cliente(lista.get(i), "clients[" + i + "]"));
        }
        try {
            return new RegistroClientes(clientes);
        } catch (IllegalArgumentException e) {
            throw invalida(e.getMessage());
        }
    }

    private JsonNode leerArbol(byte[] contenido) {
        try {
            JsonNode raiz = mapper.readTree(contenido);
            if (raiz == null || raiz.isMissingNode()) {
                throw invalida("el archivo está vacío");
            }
            return raiz;
        } catch (JsonParseException e) {
            if (e.getOriginalMessage() != null && e.getOriginalMessage().startsWith("Duplicate field")) {
                throw invalida("el JSON tiene claves duplicadas");
            }
            throw invalida("no es JSON válido");
        } catch (JsonProcessingException e) {
            throw invalida("no es JSON válido");
        } catch (IOException e) {
            throw invalida("no se pudo leer");
        }
    }

    private ClienteRegistrado cliente(JsonNode nodo, String posicion) {
        if (!nodo.isObject()) {
            throw invalida(posicion + " debe ser un objeto");
        }
        Iterator<String> campos = nodo.fieldNames();
        while (campos.hasNext()) {
            String campo = campos.next();
            if (!CAMPOS_CLIENTE.contains(campo)) {
                throw invalida(posicion + " tiene un campo desconocido");
            }
        }
        ClientId clientId = convertir(texto(nodo, "client_id", posicion), ClientId::new, posicion + ".client_id");
        HashSecreto hash = convertir(texto(nodo, "secret_hash", posicion), HashSecreto::new, posicion + ".secret_hash");
        List<Scope> scopes = lista(nodo, "scopes", posicion, Scope::new);
        List<Audiencia> audiencias = lista(nodo, "audiences", posicion, Audiencia::desde);
        try {
            return new ClienteRegistrado(clientId, hash, scopes, audiencias);
        } catch (IllegalArgumentException e) {
            throw invalida(posicion + ": " + e.getMessage());
        }
    }

    private static String texto(JsonNode nodo, String campo, String posicion) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || !valor.isTextual()) {
            throw invalida(posicion + "." + campo + " es obligatorio y debe ser texto");
        }
        return valor.textValue();
    }

    private static <T> List<T> lista(JsonNode nodo, String campo, String posicion, Function<String, T> conversor) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || !valor.isArray()) {
            throw invalida(posicion + "." + campo + " es obligatorio y debe ser una lista");
        }
        List<T> resultado = new ArrayList<>();
        for (int i = 0; i < valor.size(); i++) {
            JsonNode elemento = valor.get(i);
            String ruta = posicion + "." + campo + "[" + i + "]";
            if (!elemento.isTextual()) {
                throw invalida(ruta + " debe ser texto");
            }
            resultado.add(convertir(elemento.textValue(), conversor, ruta));
        }
        return resultado;
    }

    // Los mensajes de los Value Objects no incluyen el valor recibido, así que nunca exponen un hash.
    private static <T> T convertir(String valor, Function<String, T> conversor, String ruta) {
        try {
            return conversor.apply(valor);
        } catch (IllegalArgumentException e) {
            throw invalida(ruta + ": " + e.getMessage());
        }
    }

    private static ConfiguracionInvalidaException invalida(String detalle) {
        return new ConfiguracionInvalidaException(ORIGEN + ": " + detalle);
    }
}
