package com.zerotrust.authserver.controller;

import com.zerotrust.authserver.exception.PeticionMalFormadaException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

public class DecodificadorBasic {

    private static final String ESQUEMA = "basic";

    public record CredencialesBasic(String clientId, String secreto) {
    }

    public static boolean esBasic(String cabecera) {
        String esquema = cabecera.strip().split(" ", 2)[0];
        return ESQUEMA.equals(esquema.toLowerCase(Locale.ROOT));
    }

    public CredencialesBasic decodificar(String cabecera) {
        String[] partes = cabecera.strip().split(" +", 2);
        if (partes.length != 2 || !ESQUEMA.equals(partes[0].toLowerCase(Locale.ROOT))) {
            throw new PeticionMalFormadaException();
        }
        byte[] decodificado = base64(partes[1]);
        String bytesComoTexto = new String(decodificado, StandardCharsets.ISO_8859_1);
        int separador = bytesComoTexto.indexOf(':');
        if (separador < 0) {
            throw new PeticionMalFormadaException();
        }
        // RFC 6749 §2.3.1: cada parte va codificada como form-urlencoded antes del Base64.
        String clientId = LectorFormulario.decodificar(bytesComoTexto.substring(0, separador));
        String secreto = decodificarConClientId(bytesComoTexto.substring(separador + 1), clientId);
        return new CredencialesBasic(clientId, secreto);
    }

    private static String decodificarConClientId(String parte, String clientId) {
        try {
            return LectorFormulario.decodificar(parte);
        } catch (PeticionMalFormadaException e) {
            throw new PeticionMalFormadaException(Optional.ofNullable(clientId).filter(id -> !id.isEmpty()).orElse(null));
        }
    }

    private static byte[] base64(String valor) {
        try {
            return Base64.getDecoder().decode(valor);
        } catch (IllegalArgumentException e) {
            throw new PeticionMalFormadaException();
        }
    }
}
