package com.zerotrust.votingapi.adapter.web;

import com.zerotrust.votingapi.domain.SolicitudVotoInvalidaException;
import com.zerotrust.votingapi.domain.VotanteYaInscritoException;
import com.zerotrust.votingapi.exception.GlobalExceptionHandler;
import com.zerotrust.votingapi.service.ServicioPadron;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminVotantesControllerTest {

    private static final String TOKEN = "token-de-prueba-0123456789abcdefghij";
    private static final String CUERPO = "{\"cedula\":\"1020304051\",\"fullName\":\"Ana Pérez\",\"region\":\"norte\"}";

    @Mock
    private ServicioPadron servicio;

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new AdminVotantesController(servicio, TOKEN))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("Sin token o con un token distinto responde 401 y no inscribe")
    void sinToken() throws Exception {
        mvc().perform(post("/internal/admin/voters").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
        mvc().perform(post("/internal/admin/voters").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .header("X-Admin-Token", TOKEN + "x"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("Con token inscribe y responde 201 sin la cédula ni el UUID")
    void inscribe() throws Exception {
        mvc().perform(post("/internal/admin/voters").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .header("X-Admin-Token", TOKEN))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("{\"registered\":true}", true));

        verify(servicio).inscribir("1020304051", "Ana Pérez", "norte");
    }

    @Test
    @DisplayName("Una cédula enviada como número llega como ausente (perdería los ceros iniciales)")
    void cedulaNumerica() throws Exception {
        doThrow(new SolicitudVotoInvalidaException(List.of("cedula"))).when(servicio).inscribir(isNull(), any(), any());

        mvc().perform(post("/internal/admin/voters").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":1020304051,\"fullName\":\"Ana\",\"region\":\"norte\"}")
                        .header("X-Admin-Token", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"invalid_request\",\"fields\":[\"cedula\"]}", true));
    }

    @Test
    @DisplayName("Un documento ya inscrito responde 409 con el mensaje")
    void yaInscrito() throws Exception {
        doThrow(new VotanteYaInscritoException()).when(servicio).inscribir(any(), any(), any());

        mvc().perform(post("/internal/admin/voters").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .header("X-Admin-Token", TOKEN))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"error\":\"already_registered\","
                        + "\"message\":\"Este documento ya está inscrito en el padrón de esta elección.\"}", true));
    }

    @Test
    @DisplayName("No arranca con un token de menos de 32 caracteres")
    void tokenCorto() {
        assertThrows(IllegalStateException.class, () -> new AdminVotantesController(servicio, "corto"));
    }
}
