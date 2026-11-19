package com.zerotrust.authserver.adapter.web;

import com.zerotrust.authserver.exception.PeticionMalFormadaException;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LectorFormulario {

    static final Set<String> CONOCIDOS = Set.of("grant_type", "scope", "client_id", "client_secret");

    // Devuelve solo los parámetros conocidos; los desconocidos se ignoran aunque estén repetidos.
    public Map<String, String> leer(byte[] cuerpo) {
        // ISO-8859-1 conserva cada byte como un carácter; el UTF-8 se valida al decodificar cada valor.
        String crudo = new String(cuerpo, StandardCharsets.ISO_8859_1);
        Map<String, String> conocidos = new HashMap<>();
        for (String par : crudo.split("&", -1)) {
            int igual = par.indexOf('=');
            if (igual < 0) {
                throw new PeticionMalFormadaException();
            }
            String nombre = decodificar(par.substring(0, igual));
            String valor = decodificar(par.substring(igual + 1));
            if (CONOCIDOS.contains(nombre) && conocidos.put(nombre, valor) != null) {
                throw new PeticionMalFormadaException();
            }
        }
        return conocidos;
    }

    static String decodificar(String bytesComoTexto) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < bytesComoTexto.length(); i++) {
            char c = bytesComoTexto.charAt(i);
            if (c == '+') {
                bytes.write(' ');
            } else if (c == '%') {
                if (i + 2 >= bytesComoTexto.length()) {
                    throw new PeticionMalFormadaException();
                }
                bytes.write(hexADecimal(bytesComoTexto.charAt(i + 1)) * 16 + hexADecimal(bytesComoTexto.charAt(i + 2)));
                i += 2;
            } else {
                bytes.write(c);
            }
        }
        return utf8Estricto(bytes.toByteArray());
    }

    private static int hexADecimal(char c) {
        int valor = Character.digit(c, 16);
        if (valor < 0) {
            throw new PeticionMalFormadaException();
        }
        return valor;
    }

    private static String utf8Estricto(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new PeticionMalFormadaException();
        }
    }
}
