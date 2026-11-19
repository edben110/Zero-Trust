package com.zerotrust.voterregistration.security;

import com.zerotrust.voterregistration.domain.Cedula;
import com.zerotrust.voterregistration.domain.ElectionId;
import com.zerotrust.voterregistration.domain.VoterId;
import com.zerotrust.voterregistration.port.DerivadorVoterId;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.UUID;

// VoterId = UUID versión 8 (uso personalizado, RFC 9562) formado con los primeros 128 bits de
// HMAC-SHA-256(clave, "zerotrust-voter-v1" 0x00 electionId 0x00 cédula), con los bits de versión y variante.
// Sin la clave no se puede calcular el UUID de una cédula ni recuperar la cédula a partir del UUID.
// La clave debe mantenerse igual durante toda la elección y no puede salir del servidor.
// Copia idéntica del algoritmo de voting_api: ambos deben dar el mismo UUID para la misma cédula y elección.
public final class DerivadorVoterIdHmac implements DerivadorVoterId {

    static final String ALGORITMO = "HmacSHA256";
    static final int BYTES_MINIMOS_CLAVE = 32;
    private static final byte[] CONTEXTO = "zerotrust-voter-v1".getBytes(StandardCharsets.UTF_8);

    private final SecretKeySpec clave;

    public DerivadorVoterIdHmac(byte[] clave) {
        if (clave == null || clave.length < BYTES_MINIMOS_CLAVE) {
            throw new IllegalArgumentException("la clave HMAC debe tener al menos " + BYTES_MINIMOS_CLAVE + " bytes");
        }
        this.clave = new SecretKeySpec(clave.clone(), ALGORITMO);
    }

    public static DerivadorVoterIdHmac desdeBase64(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("la clave HMAC no puede estar vacía");
        }
        try {
            return new DerivadorVoterIdHmac(Base64.getDecoder().decode(texto.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("la clave HMAC debe estar en Base64 y tener al menos "
                    + BYTES_MINIMOS_CLAVE + " bytes");
        }
    }

    @Override
    public VoterId derivar(Cedula cedula, ElectionId eleccion) {
        byte[] hash = hmac(cedula, eleccion);
        hash[6] = (byte) ((hash[6] & 0x0F) | 0x80);   // versión 8
        hash[8] = (byte) ((hash[8] & 0x3F) | 0x80);   // variante RFC 9562
        long mayor = 0;
        long menor = 0;
        for (int i = 0; i < 8; i++) {
            mayor = (mayor << 8) | (hash[i] & 0xFF);
            menor = (menor << 8) | (hash[8 + i] & 0xFF);
        }
        return new VoterId(new UUID(mayor, menor).toString());
    }

    private byte[] hmac(Cedula cedula, ElectionId eleccion) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);   // Mac no es thread-safe: una instancia por llamada
            mac.init(clave);
            mac.update(CONTEXTO);
            mac.update((byte) 0);
            mac.update(eleccion.valor().getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            mac.update(cedula.valor().getBytes(StandardCharsets.UTF_8));
            return mac.doFinal();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("no se pudo calcular el HMAC");
        }
    }
}
