package com.zerotrust.votingapi.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositorioCatalogoTest {
    @TempDir Path dir;
    private final ObjectMapper json = new ObjectMapper();
    private static final String UNO = "0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8";
    private static final String DOS = "1c2d3e4f-5061-4b72-8c83-a4b5c6d7e8f9";

    @Test
    void cambiosPersistenYNoSePierdeElMinimo() throws Exception {
        Path seed = dir.resolve("seed.json");
        Path estado = dir.resolve("datos/catalogo.json");
        Files.writeString(seed, "{\"candidates\":["
                + "{\"candidateId\":\"" + UNO + "\",\"name\":\"Primero\",\"list\":\"Lista 1\"},"
                + "{\"candidateId\":\"" + DOS + "\",\"name\":\"Segundo\",\"list\":\"Lista 2\"}],"
                + "\"regions\":[\"norte\"]}");
        RepositorioCatalogo repo = new RepositorioCatalogo(seed, estado, json);
        String nuevo = repo.crear(" Tercero ", "Lista 3").candidatos().get(2).candidateId().valor();
        repo.editar(nuevo, "Actualizado", "Lista 4");
        repo.eliminar(UNO);
        assertThatThrownBy(() -> repo.eliminar(DOS))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("al menos dos");
        assertThat(new RepositorioCatalogo(seed, estado, json).actual().candidatos())
                .extracting(c -> c.nombre()).containsExactly("Segundo", "Actualizado");
    }
}
