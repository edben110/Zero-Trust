package com.zerotrust.votingapi.adapter.web;

import com.zerotrust.votingapi.adapter.config.RepositorioCatalogo;
import com.zerotrust.votingapi.domain.ElectionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminEleccionControllerTest {
    private static final String TOKEN = "token-seguro-de-prueba-0123456789abcdef";

    @Mock JdbcTemplate jdbc;
    @Mock PlatformTransactionManager gestor;
    @Mock RepositorioCatalogo catalogo;
    @Mock TransactionStatus estado;

    @Test
    void sinTokenNoTocaLaBaseDeDatos() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new AdminEleccionController(
                TOKEN, jdbc, gestor, new ElectionId("eleccion-1"), catalogo)).build();
        mvc.perform(post("/internal/admin/election/reset")).andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/admin/election/reset").header("X-Admin-Token", "otro-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(jdbc, gestor, catalogo);
    }

    @Test
    void resetActualizaLaJornadaYLiberaLosVotantesEnUnaTransaccion() throws Exception {
        var inicial = new com.zerotrust.votingapi.domain.Catalogo(
                java.util.List.of(
                        new com.zerotrust.votingapi.domain.Candidato(
                                new com.zerotrust.votingapi.domain.CandidateId(UUID.randomUUID().toString()), "Ana", "A"),
                        new com.zerotrust.votingapi.domain.Candidato(
                                new com.zerotrust.votingapi.domain.CandidateId(UUID.randomUUID().toString()), "Beto", "B")),
                java.util.List.of(new com.zerotrust.votingapi.domain.Region("norte")));
        when(catalogo.actual()).thenReturn(inicial);
        when(gestor.getTransaction(any())).thenReturn(estado);
        when(jdbc.update(eq("UPDATE election_state SET run_id=? WHERE id=1"), any(UUID.class))).thenReturn(1);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminEleccionController(
                TOKEN, jdbc, gestor, new ElectionId("eleccion-1"), catalogo)).build();
        mvc.perform(post("/internal/admin/election/reset").header("X-Admin-Token", TOKEN))
                .andExpect(status().isOk());
        var orden = inOrder(jdbc, gestor);
        orden.verify(gestor).getTransaction(any());
        orden.verify(jdbc).update(eq("UPDATE election_state SET run_id=? WHERE id=1"), any(UUID.class));
        orden.verify(jdbc).update("DELETE FROM voter_election_usage WHERE election_id=?", "eleccion-1");
        orden.verify(gestor).commit(estado);
    }
}
