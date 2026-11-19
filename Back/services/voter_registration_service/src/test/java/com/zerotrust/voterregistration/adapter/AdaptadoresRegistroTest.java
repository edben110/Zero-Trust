package com.zerotrust.voterregistration.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.voterregistration.adapter.archivo.LectorArchivoVotantes;
import com.zerotrust.voterregistration.adapter.crypto.DerivadorVoterIdHmac;
import com.zerotrust.voterregistration.adapter.kafka.PublicadorElegiblesKafka;
import com.zerotrust.voterregistration.adapter.persistence.MapperVotante;
import com.zerotrust.voterregistration.adapter.persistence.RepositorioPadronJpa;
import com.zerotrust.voterregistration.adapter.persistence.VotanteJpaRepository;
import com.zerotrust.voterregistration.adapter.persistence.VotanteRegistradoEntity;
import com.zerotrust.voterregistration.domain.ElectionId;
import com.zerotrust.voterregistration.domain.PadronInicial;
import com.zerotrust.voterregistration.exception.PadronInvalidoException;
import com.zerotrust.voterregistration.domain.Region;
import com.zerotrust.voterregistration.domain.RegistroVotanteCrudo;
import com.zerotrust.voterregistration.domain.VotanteRegistrado;
import com.zerotrust.voterregistration.domain.VoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdaptadoresRegistroTest {

    private static final String ID_1 = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final String ID_2 = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f2a3b4c5d";
    private static final Instant AHORA = Instant.parse("2026-11-14T08:00:00Z");

    private static VotanteRegistrado votante(String id) {
        return new VotanteRegistrado(new VoterId(id), new Region("norte"), AHORA);
    }

    @Nested
    @DisplayName("LectorArchivoVotantes")
    class Lector {

        private static final String CEDULA = "0012345678";
        // Mismo vector que DerivadorVoterIdHmacTest de voting_api: clave = bytes 0..31, elección "elecciones-2026".
        private static final String VOTER_ID_CEDULA = "fae9cb04-ba2b-87ef-882c-f39dd70dbaa1";

        private final LectorArchivoVotantes lector = new LectorArchivoVotantes(
                new DerivadorVoterIdHmac(clavePrueba()), new ElectionId("elecciones-2026"));

        private static byte[] clavePrueba() {
            byte[] clave = new byte[32];
            for (int i = 0; i < clave.length; i++) {
                clave[i] = (byte) i;
            }
            return clave;
        }

        @Test
        @DisplayName("Debe reemplazar la cédula por el mismo UUID que devuelve POST /voter-id de voting_api")
        void lee() {
            List<RegistroVotanteCrudo> crudos = lector.interpretar(("[{\"cedula\":\"" + CEDULA
                    + "\",\"region\":\"norte\"}]").getBytes(StandardCharsets.UTF_8));

            assertThat(crudos).containsExactly(new RegistroVotanteCrudo(VOTER_ID_CEDULA, "norte"));
            assertThat(crudos.get(0).toString()).doesNotContain(CEDULA);
        }

        @Test
        @DisplayName("Dos escrituras equivalentes de la cédula dan el mismo UUID")
        void equivalentes() {
            List<RegistroVotanteCrudo> crudos = lector.interpretar(
                    ("[{\"cedula\":\"1.234.567.890\",\"region\":\"norte\"},"
                            + "{\"cedula\":\" 1234567890 \",\"region\":\"sur\"}]")
                            .getBytes(StandardCharsets.UTF_8));

            assertThat(crudos.get(0).voterId()).isEqualTo(crudos.get(1).voterId());
        }

        @Test
        @DisplayName("Una cédula ausente, numérica o mal formada invalida el archivo sin revelar su valor")
        void cedulaInvalida() {
            for (String registro : List.of("{\"region\":\"norte\"}",
                    "{\"cedula\":12345678,\"region\":\"norte\"}",
                    "{\"cedula\":\"98765x4321\",\"region\":\"norte\"}")) {
                PadronInvalidoException error = assertThrows(PadronInvalidoException.class,
                        () -> lector.interpretar(("[" + registro + "]").getBytes(StandardCharsets.UTF_8)));

                assertThat(error.getMessage()).contains("registro [0]").contains("cedula").doesNotContain("98765");
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "no es json", "{}", "[1]", "[\"texto\"]"})
        @DisplayName("Debe rechazar un archivo que no es una lista de objetos")
        void rechaza(String json) {
            assertThrows(PadronInvalidoException.class, () -> lector.interpretar(json.getBytes(StandardCharsets.UTF_8)));
        }

        @Test
        @DisplayName("El padrón de ejemplo del repositorio es válido")
        void ejemplo() throws Exception {
            List<RegistroVotanteCrudo> crudos = lector.interpretar(Files.readAllBytes(Path.of("config", "votantes.ejemplo.json")));

            assertThat(PadronInicial.desde(crudos, AHORA).votantes()).hasSizeGreaterThanOrEqualTo(5);
        }
    }
    @Nested
    @DisplayName("MapperVotante")
    class Mapper {

        @Test
        @DisplayName("Debe convertir del dominio a la entidad y de vuelta sin perder datos")
        void idaYVuelta() {
            VotanteRegistrado original = votante(ID_1);

            VotanteRegistradoEntity entidad = MapperVotante.aEntidad(original);

            assertThat(entidad.getVoterId()).isEqualTo(ID_1);
            assertThat(MapperVotante.aDominio(entidad)).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("RepositorioPadronJpa")
    @ExtendWith(MockitoExtension.class)
    class Repositorio {

        @Mock
        private VotanteJpaRepository jpa;

        @Test
        @DisplayName("Solo debe insertar los votantes que no existían")
        void soloNuevos() {
            when(jpa.buscarVoterIdsExistentes(anyCollection())).thenReturn(Set.of(ID_1));
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<VotanteRegistradoEntity>> insertados = ArgumentCaptor.forClass(List.class);

            int nuevos = new RepositorioPadronJpa(jpa).guardarNuevos(List.of(votante(ID_1), votante(ID_2)));

            verify(jpa, times(1)).saveAll(insertados.capture());
            assertThat(insertados.getValue()).extracting(VotanteRegistradoEntity::getVoterId).containsExactly(ID_2);
            assertThat(nuevos).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("PublicadorElegiblesKafka")
    @ExtendWith(MockitoExtension.class)
    class Kafka {

        @Mock
        private KafkaTemplate<String, String> plantilla;

        private PublicadorElegiblesKafka publicador() {
            return new PublicadorElegiblesKafka(plantilla, "eligible_voters", new ObjectMapper(), Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("Debe publicar con key = voterId y el payload exacto del contrato")
        void publica() {
            ArgumentCaptor<String> valor = ArgumentCaptor.forClass(String.class);
            when(plantilla.send(eq("eligible_voters"), eq(ID_1), valor.capture()))
                    .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

            publicador().publicar(votante(ID_1));

            assertThat(valor.getValue()).isEqualTo("{\"voterId\":\"" + ID_1
                    + "\",\"region\":\"norte\",\"registeredAt\":\"2026-11-14T08:00:00Z\"}");
        }

        @Test
        @DisplayName("Si Kafka falla debe lanzar un error para terminar con código distinto de 0")
        void falla() {
            when(plantilla.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));

            assertThrows(IllegalStateException.class, () -> publicador().publicar(votante(ID_1)));
        }
    }
}
