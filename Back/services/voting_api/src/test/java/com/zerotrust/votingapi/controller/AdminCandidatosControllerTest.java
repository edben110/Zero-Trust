package com.zerotrust.votingapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.config.RepositorioCatalogo;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCandidatosControllerTest {
    @TempDir Path dir;

    @Test
    void tokenObligatorioYEdicionVisibleEnCatalogo() throws Exception {
        Path inicial = dir.resolve("seed.json");
        Files.writeString(inicial, "{\"candidates\":[{\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\","
                + "\"name\":\"Inicial\",\"list\":\"Lista\"}],\"regions\":[\"norte\"]}");
        RepositorioCatalogo repo = new RepositorioCatalogo(inicial, dir.resolve("state/catalogo.json"),
                new ObjectMapper());
        String token = "a".repeat(48);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminCandidatosController(repo, token),
                new CatalogoController(repo)).build();
        var peticion = post("/internal/admin/candidates")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nuevo\",\"list\":\"Lista 2\"}");
        mvc.perform(peticion).andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/admin/candidates").header("X-Admin-Token", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nuevo\",\"list\":\"Lista 2\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.candidates.length()").value(2));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/catalog"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.candidates[1].name").value("Nuevo"));
    }
}
