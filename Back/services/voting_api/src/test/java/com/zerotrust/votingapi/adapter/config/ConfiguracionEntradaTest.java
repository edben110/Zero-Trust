package com.zerotrust.votingapi.adapter.config;

import com.zerotrust.votingapi.domain.CandidateId;
import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.domain.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguracionEntradaTest {

    private static final String CANDIDATO = "{\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\","
            + "\"name\":\"Lucía Marín Ospina\",\"list\":\"Movimiento Ceiba\"}";

    @Nested
    @DisplayName("LectorCatalogo")
    class Catalogos {

        private final LectorCatalogo lector = new LectorCatalogo();

        private Catalogo leer(String json) {
            return lector.interpretar(json.getBytes(StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("Debe leer candidatos y regiones")
        void leeValido() {
            Catalogo catalogo = leer("{\"candidates\":[" + CANDIDATO + "],\"regions\":[\"norte\",\"sur\"]}");

            assertThat(catalogo.existeCandidato(new CandidateId("0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8"))).isTrue();
            assertThat(catalogo.regiones()).containsExactly(new Region("norte"), new Region("sur"));
            assertThat(catalogo.candidatos().get(0).lista()).isEqualTo("Movimiento Ceiba");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "no es json", "[]", "{}", "{\"candidates\":[],\"regions\":[\"norte\"]}",
                "{\"candidates\":[" + CANDIDATO + "],\"regions\":[]}",
                "{\"candidates\":[" + CANDIDATO + "],\"regions\":[\"norte\"],\"extra\":1}",
                "{\"candidates\":[{\"candidateId\":\"x\",\"name\":\"A\",\"list\":\"B\"}],\"regions\":[\"norte\"]}",
                "{\"candidates\":[{\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\",\"name\":\"A\"}],\"regions\":[\"norte\"]}",
                "{\"candidates\":[" + CANDIDATO + "," + CANDIDATO + "],\"regions\":[\"norte\"]}",
                "{\"candidates\":[" + CANDIDATO + "],\"regions\":[\"norte\",\"norte\"]}",
                "{\"candidates\":[" + CANDIDATO + "],\"regions\":[1]}"})
        @DisplayName("Debe impedir el arranque con un catálogo inválido")
        void rechazaInvalidos(String json) {
            IllegalStateException error = assertThrows(IllegalStateException.class, () -> leer(json));
            assertThat(error.getMessage()).startsWith("CATALOG_FILE");
        }

        @Test
        @DisplayName("El catálogo de ejemplo del repositorio es válido y tiene las cinco regiones")
        void ejemplo() throws Exception {
            Catalogo catalogo = lector.interpretar(Files.readAllBytes(Path.of("config", "catalogo.ejemplo.json")));

            assertThat(catalogo.regiones()).extracting(Region::valor)
                    .containsExactly("norte", "sur", "oriente", "occidente", "centro");
            assertThat(catalogo.candidatos()).hasSizeGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("ResolutorIpOrigen")
    class IpOrigen {

        @Test
        @DisplayName("Sin confiar en cabeceras debe usar la dirección remota aunque llegue X-Forwarded-For")
        void sinConfianza() {
            ResolutorIpOrigen resolutor = ResolutorIpOrigen.desde(null);

            assertThat(resolutor.resolver("10.0.0.5", "203.0.113.7").valor()).isEqualTo("10.0.0.5");
        }

        @Test
        @DisplayName("Confiando en cabeceras debe usar la primera dirección de X-Forwarded-For")
        void conConfianza() {
            ResolutorIpOrigen resolutor = ResolutorIpOrigen.desde("true");

            assertThat(resolutor.resolver("10.0.0.5", " 203.0.113.7 , 10.0.0.1").valor()).isEqualTo("203.0.113.7");
            assertThat(resolutor.resolver("10.0.0.5", null).valor()).isEqualTo("10.0.0.5");
            assertThat(resolutor.resolver("10.0.0.5", " ,10.0.0.1").valor()).isEqualTo("10.0.0.5");
            assertThat(resolutor.resolver("10.0.0.5", "x".repeat(60)).valor()).isEqualTo("10.0.0.5");
        }

        @Test
        @DisplayName("TRUST_FORWARDED_HEADERS solo admite true o false")
        void variable() {
            assertThat(ResolutorIpOrigen.desde("false").resolver("10.0.0.5", "203.0.113.7").valor()).isEqualTo("10.0.0.5");
            assertThrows(IllegalStateException.class, () -> ResolutorIpOrigen.desde("si"));
        }
    }

    @Nested
    @DisplayName("OrigenesCors")
    class Cors {

        @Test
        @DisplayName("Debe leer la lista separada por comas")
        void lee() {
            assertThat(OrigenesCors.desde(" https://votacion.example , http://localhost:3000 ").origenes())
                    .containsExactly("https://votacion.example", "http://localhost:3000");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "*", "https://a.example,*", "https://*.example", "votacion.example", " , "})
        @DisplayName("Debe impedir el arranque sin orígenes, con comodín o con orígenes sin esquema")
        void rechaza(String valor) {
            assertThrows(IllegalStateException.class, () -> OrigenesCors.desde(valor));
        }

        @Test
        @DisplayName("Debe impedir el arranque si la variable no existe")
        void ausente() {
            assertThrows(IllegalStateException.class, () -> OrigenesCors.desde(null));
        }
    }
}
