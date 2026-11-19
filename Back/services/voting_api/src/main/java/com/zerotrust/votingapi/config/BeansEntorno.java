package com.zerotrust.votingapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.port.DerivadorVoterId;
import com.zerotrust.votingapi.security.DerivadorVoterIdHmac;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

// Fail-fast: si falta una variable obligatoria, el servicio no arranca.
@Configuration
public class BeansEntorno {

    @Bean
    RepositorioCatalogo catalogo(Environment entorno, ObjectMapper json) {
        String ruta = entorno.getProperty("CATALOG_FILE");
        if (ruta == null || ruta.isBlank()) {
            throw new IllegalStateException("CATALOG_FILE: variable obligatoria sin definir");
        }
        String estado = entorno.getProperty("CATALOG_STATE_FILE");
        if (estado == null || estado.isBlank()) {
            throw new IllegalStateException("CATALOG_STATE_FILE: variable obligatoria sin definir");
        }
        return new RepositorioCatalogo(Path.of(ruta), Path.of(estado), json);
    }

    @Bean
    ResolutorIpOrigen resolutorIpOrigen(Environment entorno) {
        return ResolutorIpOrigen.desde(entorno.getProperty("TRUST_FORWARDED_HEADERS"));
    }

    // La clave nunca aparece en el mensaje de error.
    @Bean
    ElectionId eleccion(Environment entorno) {
        String valor = entorno.getProperty("ELECTION_ID");
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("ELECTION_ID: variable obligatoria sin definir");
        }
        try {
            return new ElectionId(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ELECTION_ID: " + e.getMessage());
        }
    }

    @Bean
    DerivadorVoterId derivadorVoterId(Environment entorno) {
        try {
            return DerivadorVoterIdHmac.desdeBase64(entorno.getProperty("VOTER_HMAC_KEY"));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("VOTER_HMAC_KEY: " + e.getMessage());
        }
    }
}
