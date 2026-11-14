package com.zerotrust.votingapi.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DominioVotacionTest {

    static final String VOTANTE = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    static final String CANDIDATO = "0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8";
    static final String OTRO = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f2a3b4c5d";

    static Catalogo catalogo() {
        return new Catalogo(
                List.of(new Candidato(new CandidateId(CANDIDATO), "Lucía Marín Ospina", "Movimiento Ceiba")),
                List.of(new Region("norte"), new Region("sur")));
    }

    @Nested
    @DisplayName("Identificadores")
    class Identificadores {

        @Test
        @DisplayName("Deben aceptar UUID canónicos y normalizarlos a minúsculas")
        void aceptan() {
            assertThat(new VoterId(VOTANTE.toUpperCase()).valor()).isEqualTo(VOTANTE);
            assertThat(new CandidateId(CANDIDATO).valor()).isEqualTo(CANDIDATO);
            assertThat(new VoteId(OTRO).valor()).isEqualTo(OTRO);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "x", "1-1-1-1-1", "3f2b8c1e6d4a4b7e9c210a5d6e7f8a9b"})
        @DisplayName("Deben rechazar valores que no son UUID canónicos")
        void rechazan(String valor) {
            assertThrows(IllegalArgumentException.class, () -> new VoterId(valor));
            assertThrows(IllegalArgumentException.class, () -> new CandidateId(valor));
            assertThrows(IllegalArgumentException.class, () -> new VoteId(valor));
        }

        @Test
        @DisplayName("Deben rechazar nulos")
        void rechazanNulos() {
            assertThrows(IllegalArgumentException.class, () -> new VoterId(null));
            assertThrows(IllegalArgumentException.class, () -> new Region(null));
            assertThrows(IllegalArgumentException.class, () -> new SourceIp(null));
        }

        @Test
        @DisplayName("Region y SourceIp no pueden estar vacíos; SourceIp cabe en 45 caracteres")
        void regionYSourceIp() {
            assertThrows(IllegalArgumentException.class, () -> new Region(" "));
            assertThrows(IllegalArgumentException.class, () -> new SourceIp(""));
            assertThrows(IllegalArgumentException.class, () -> new SourceIp("1".repeat(46)));
            assertThat(new SourceIp("203.0.113.7").valor()).isEqualTo("203.0.113.7");
        }
    }

    @Nested
    @DisplayName("Catalogo")
    class CatalogoTest {

        @Test
        @DisplayName("Debe responder si un candidato o una región existen")
        void consultas() {
            Catalogo catalogo = catalogo();

            assertThat(catalogo.existeCandidato(new CandidateId(CANDIDATO))).isTrue();
            assertThat(catalogo.existeCandidato(new CandidateId(OTRO))).isFalse();
            assertThat(catalogo.existeRegion(new Region("norte"))).isTrue();
            assertThat(catalogo.existeRegion(new Region("Norte"))).isFalse();
        }

        @Test
        @DisplayName("Debe rechazar un catálogo sin candidatos o sin regiones, o con duplicados")
        void invariantes() {
            Candidato candidato = new Candidato(new CandidateId(CANDIDATO), "A", "B");
            assertThrows(IllegalArgumentException.class, () -> new Catalogo(List.of(), List.of(new Region("norte"))));
            assertThrows(IllegalArgumentException.class, () -> new Catalogo(List.of(candidato), List.of()));
            assertThrows(IllegalArgumentException.class, () -> new Catalogo(List.of(candidato, candidato), List.of(new Region("norte"))));
            assertThrows(IllegalArgumentException.class, () -> new Catalogo(List.of(candidato), List.of(new Region("norte"), new Region("norte"))));
        }

        @Test
        @DisplayName("Un candidato necesita nombre y lista, y el catálogo no expone sus listas internas")
        void candidatoEInmutabilidad() {
            assertThrows(IllegalArgumentException.class, () -> new Candidato(new CandidateId(CANDIDATO), " ", "B"));
            assertThrows(IllegalArgumentException.class, () -> new Candidato(new CandidateId(CANDIDATO), "A", ""));
            Catalogo catalogo = catalogo();
            assertThrows(UnsupportedOperationException.class, () -> catalogo.regiones().add(new Region("centro")));
            assertThrows(UnsupportedOperationException.class, () -> catalogo.candidatos().clear());
        }
    }

    @Nested
    @DisplayName("SolicitudVoto")
    class SolicitudVotoTest {

        @Test
        @DisplayName("Debe construirse con datos válidos y presentes en el catálogo")
        void valida() {
            SolicitudVoto solicitud = SolicitudVoto.desde(VOTANTE, CANDIDATO, "norte", catalogo());

            assertThat(solicitud.voterId()).isEqualTo(new VoterId(VOTANTE));
            assertThat(solicitud.region()).isEqualTo(new Region("norte"));
        }

        @Test
        @DisplayName("Debe reunir todos los campos inválidos")
        void reuneCampos() {
            SolicitudVotoInvalidaException error = assertThrows(SolicitudVotoInvalidaException.class,
                    () -> SolicitudVoto.desde("x", OTRO, "centro", catalogo()));

            assertThat(error.campos()).containsExactly("voterId", "candidateId", "region");
        }

        @Test
        @DisplayName("Los campos ausentes también son inválidos")
        void ausentes() {
            SolicitudVotoInvalidaException error = assertThrows(SolicitudVotoInvalidaException.class,
                    () -> SolicitudVoto.desde(null, null, null, catalogo()));

            assertThat(error.campos()).containsExactly("voterId", "candidateId", "region");
        }
    }

    @Nested
    @DisplayName("VotoEmitido")
    class VotoEmitidoTest {

        @Test
        @DisplayName("Debe reunir la solicitud, la IP, el voteId y el instante")
        void emite() {
            Instant ahora = Instant.parse("2026-11-14T15:00:00Z");
            SolicitudVoto solicitud = SolicitudVoto.desde(VOTANTE, CANDIDATO, "sur", catalogo());

            VotoEmitido voto = VotoEmitido.emitir(solicitud, new SourceIp("198.51.100.4"), new VoteId(OTRO), ahora);

            assertThat(voto.voteId()).isEqualTo(new VoteId(OTRO));
            assertThat(voto.candidateId()).isEqualTo(new CandidateId(CANDIDATO));
            assertThat(voto.region()).isEqualTo(new Region("sur"));
            assertThat(voto.sourceIp()).isEqualTo(new SourceIp("198.51.100.4"));
            assertThat(voto.timestamp()).isEqualTo(ahora);
        }
    }
}
