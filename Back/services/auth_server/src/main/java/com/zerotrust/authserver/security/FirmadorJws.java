package com.zerotrust.authserver.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.zerotrust.authserver.domain.Audiencia;
import com.zerotrust.authserver.domain.ClaimsToken;
import com.zerotrust.authserver.domain.IdClave;
import com.zerotrust.authserver.port.FirmadorTokens;

import java.security.interfaces.RSAPrivateKey;
import java.util.LinkedHashMap;
import java.util.Map;

// JWTClaimsSet de nimbus convierte una audiencia única en string; RF-4 exige array, así que el payload es propio.
public class FirmadorJws implements FirmadorTokens {

    private final RSASSASigner firmante;
    private final JWSHeader cabecera;
    private final ObjectMapper json = new ObjectMapper();

    public FirmadorJws(RSAPrivateKey clavePrivada, IdClave kid) {
        this.firmante = new RSASSASigner(clavePrivada);
        this.cabecera = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid.valor()).type(JOSEObjectType.JWT).build();
    }

    @Override
    public String firmar(ClaimsToken claims) {
        JWSObject jws = new JWSObject(cabecera, new Payload(payload(claims)));
        try {
            jws.sign(firmante);
        } catch (JOSEException e) {
            throw new IllegalStateException("no se pudo firmar el token", e);
        }
        return jws.serialize();
    }

    private String payload(ClaimsToken claims) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("iss", claims.iss());
        campos.put("sub", claims.sub());
        campos.put("aud", claims.aud().stream().map(Audiencia::valor).toList());
        claims.scope().ifPresent(scope -> campos.put("scope", scope));
        campos.put("iat", claims.iat());
        campos.put("exp", claims.exp());
        try {
            return json.writeValueAsString(campos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("no se pudo serializar el payload", e);
        }
    }
}
