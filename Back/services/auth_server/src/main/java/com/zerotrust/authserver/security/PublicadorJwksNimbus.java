package com.zerotrust.authserver.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.zerotrust.authserver.domain.IdClave;
import com.zerotrust.authserver.port.PublicadorClaves;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

public class PublicadorJwksNimbus implements PublicadorClaves {

    private final Map<String, Object> jwkSet;

    public PublicadorJwksNimbus(RSAPublicKey clavePublica, IdClave kid) {
        RSAKey clave = new RSAKey.Builder(clavePublica)
                .keyID(kid.valor())
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .build();
        Map<String, Object> publico = new JWKSet(clave).toJSONObject(true);
        this.jwkSet = copiaInmutable(publico);
    }

    @Override
    public Map<String, Object> jwkSetPublico() {
        return jwkSet;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copiaInmutable(Map<String, Object> original) {
        List<Map<String, Object>> claves = ((List<Map<String, Object>>) original.get("keys")).stream()
                .map(Map::copyOf)
                .toList();
        return Map.of("keys", claves);
    }
}
