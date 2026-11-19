package com.zerotrust.authserver.controller;

import com.zerotrust.authserver.exception.PeticionMalFormadaException;
import com.zerotrust.authserver.domain.SecretoCliente;
import com.zerotrust.authserver.service.PeticionToken;

import java.util.List;
import java.util.Locale;
import java.util.Map;

// Aplica las condiciones de invalid_request de RF-10 en su orden; client_id y scope salen crudos.
public class InterpretePeticionToken {

    private static final String FORMULARIO = "application/x-www-form-urlencoded";

    private final LectorFormulario lector = new LectorFormulario();
    private final DecodificadorBasic decodificadorBasic = new DecodificadorBasic();

    public PeticionToken interpretar(String contentType, List<String> autorizaciones, byte[] cuerpo) {
        if (!esFormulario(contentType) || cuerpo == null || cuerpo.length == 0) {
            throw new PeticionMalFormadaException();
        }
        Map<String, String> parametros = lector.leer(cuerpo);
        String clientIdCuerpo = valor(parametros, "client_id");
        String grantType = valor(parametros, "grant_type");
        if (grantType == null) {
            throw new PeticionMalFormadaException(clientIdCuerpo);
        }
        String scope = valor(parametros, "scope");
        if (scope != null && contieneEspacioDistintoDeU0020(scope)) {
            throw new PeticionMalFormadaException(clientIdCuerpo);
        }
        if (autorizaciones.size() > 1) {
            throw new PeticionMalFormadaException(clientIdCuerpo);
        }
        if (autorizaciones.size() == 1) {
            return conBasic(autorizaciones.get(0), parametros, grantType, scope, clientIdCuerpo);
        }
        String secreto = valor(parametros, "client_secret");
        if (clientIdCuerpo == null || secreto == null) {
            throw new PeticionMalFormadaException(clientIdCuerpo);
        }
        return new PeticionToken(grantType, clientIdCuerpo, new SecretoCliente(secreto), scope, false);
    }

    private PeticionToken conBasic(String cabecera, Map<String, String> parametros, String grantType, String scope,
                                   String clientIdCuerpo) {
        if (!DecodificadorBasic.esBasic(cabecera)) {
            throw new PeticionMalFormadaException(clientIdCuerpo);
        }
        DecodificadorBasic.CredencialesBasic credenciales = decodificadorBasic.decodificar(cabecera);
        String clientId = credenciales.clientId().isEmpty() ? null : credenciales.clientId();
        if (parametros.containsKey("client_id") || parametros.containsKey("client_secret")) {
            throw new PeticionMalFormadaException(clientId);
        }
        if (clientId == null || credenciales.secreto().isEmpty()) {
            throw new PeticionMalFormadaException(clientId);
        }
        return new PeticionToken(grantType, clientId, new SecretoCliente(credenciales.secreto()), scope, true);
    }

    private static boolean esFormulario(String contentType) {
        if (contentType == null) {
            return false;
        }
        String tipo = contentType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        return FORMULARIO.equals(tipo);
    }

    // RFC 6749 §3.1: un parámetro sin valor se trata como si no se hubiera enviado.
    private static String valor(Map<String, String> parametros, String nombre) {
        String valor = parametros.get(nombre);
        return valor == null || valor.isEmpty() ? null : valor;
    }

    private static boolean contieneEspacioDistintoDeU0020(String texto) {
        return texto.codePoints().anyMatch(c -> c != ' ' && (Character.isWhitespace(c) || Character.isSpaceChar(c)));
    }
}
