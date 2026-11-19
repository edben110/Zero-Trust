package com.zerotrust.voterregistration.config;

import com.zerotrust.voterregistration.security.DerivadorVoterIdHmac;
import com.zerotrust.voterregistration.domain.ElectionId;
import com.zerotrust.voterregistration.domain.PadronInicial;
import com.zerotrust.voterregistration.exception.PadronInvalidoException;
import com.zerotrust.voterregistration.domain.Region;
import com.zerotrust.voterregistration.domain.RegistroVotanteCrudo;
import com.zerotrust.voterregistration.domain.VotanteRegistrado;
import com.zerotrust.voterregistration.domain.VoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("LectorArchivoVotantes")
class LectorArchivoVotantesTest {

    private static final Instant AHORA = Instant.parse("2026-11-14T08:00:00Z");

    private static VotanteRegistrado votante(String id) {
        return new VotanteRegistrado(new VoterId(id), new Region("norte"), AHORA);
    }

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
