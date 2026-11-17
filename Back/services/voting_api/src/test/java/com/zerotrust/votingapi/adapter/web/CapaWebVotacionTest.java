package com.zerotrust.votingapi.adapter.web;

import com.zerotrust.votingapi.adapter.config.ResolutorIpOrigen;
import com.zerotrust.votingapi.domain.Candidato;
import com.zerotrust.votingapi.domain.CandidateId;
import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.domain.PublicacionNoDisponibleException;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.domain.ValidacionNoDisponibleException;
import com.zerotrust.votingapi.domain.VotoEmitido;
import com.zerotrust.votingapi.exception.GlobalExceptionHandler;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.ValidadorVotante;
import com.zerotrust.votingapi.service.ServicioVotacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CapaWebVotacionTest {

    private static final String VOTANTE = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final String CANDIDATO = "0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8";
    private static final UUID VOTE_ID = UUID.fromString("11111111-2222-4333-8444-555555555555");
    private static final String CUERPO = "{\"voterId\":\"" + VOTANTE + "\",\"candidateId\":\"" + CANDIDATO
            + "\",\"region\":\"norte\"}";

    @Mock
    private ValidadorVotante validador;
    @Mock
    private PublicadorVotos publicador;

    private MockMvc mvc(boolean confiarEnCabeceras) {
        Catalogo catalogo = new Catalogo(
                List.of(new Candidato(new CandidateId(CANDIDATO), "Lucía Marín Ospina", "Movimiento Ceiba")),
                List.of(new Region("norte"), new Region("sur")));
        ServicioVotacion servicio = new ServicioVotacion(validador, publicador,
                Clock.fixed(Instant.parse("2026-11-14T15:00:00Z"), ZoneOffset.UTC), () -> VOTE_ID);
        VotoController votos = new VotoController(servicio, catalogo,
                ResolutorIpOrigen.desde(String.valueOf(confiarEnCabeceras)));
        return MockMvcBuilders.standaloneSetup(votos, new CatalogoController(catalogo))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Un voto válido de un votante habilitado responde 200 con voteId y receivedAt y publica una vez")
    void votoAceptado() throws Exception {
        when(validador.estaHabilitado(any())).thenReturn(true);
        ArgumentCaptor<VotoEmitido> voto = ArgumentCaptor.forClass(VotoEmitido.class);

        mvc(false).perform(post("/vote").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .header("X-Forwarded-For", "203.0.113.7"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"voteId\":\"" + VOTE_ID + "\",\"receivedAt\":\"2026-11-14T15:00:00Z\"}", true));

        verify(publicador, times(1)).publicar(voto.capture());
        assertThat(voto.getValue().sourceIp()).isEqualTo(new SourceIp("127.0.0.1"));
    }

    @Test
    @DisplayName("Con TRUST_FORWARDED_HEADERS=true la IP sale de X-Forwarded-For")
    void ipDeCabecera() throws Exception {
        when(validador.estaHabilitado(any())).thenReturn(true);
        ArgumentCaptor<VotoEmitido> voto = ArgumentCaptor.forClass(VotoEmitido.class);

        mvc(true).perform(post("/vote").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1"));

        verify(publicador).publicar(voto.capture());
        assertThat(voto.getValue().sourceIp()).isEqualTo(new SourceIp("203.0.113.7"));
    }

    @Test
    @DisplayName("Campos inválidos responden 400 con su lista, sin RPC ni publicación")
    void camposInvalidos() throws Exception {
        mvc(false).perform(post("/vote").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voterId\":\"x\",\"candidateId\":\"" + CANDIDATO + "\",\"region\":\"centro\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"invalid_request\",\"fields\":[\"voterId\",\"region\"]}", true));

        verifyNoInteractions(validador, publicador);
    }

    @Test
    @DisplayName("Un cuerpo ilegible, vacío o que no es JSON responde 400 invalid_request")
    void cuerpoIlegible() throws Exception {
        mvc(false).perform(post("/vote").contentType(MediaType.APPLICATION_JSON).content("{no json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.fields").isArray());
        mvc(false).perform(post("/vote").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc(false).perform(post("/vote").contentType(MediaType.TEXT_PLAIN).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));

        verifyNoInteractions(validador, publicador);
    }

    @Test
    @DisplayName("Un votante no habilitado responde 403 voter_not_eligible y no publica")
    void noHabilitado() throws Exception {
        when(validador.estaHabilitado(any())).thenReturn(false);

        mvc(false).perform(post("/vote").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"error\":\"voter_not_eligible\"}", true));

        verify(publicador, never()).publicar(any());
    }

    @Test
    @DisplayName("Sin respuesta RPC o con Kafka caído responde 503 validation_unavailable")
    void noDisponible() throws Exception {
        when(validador.estaHabilitado(any())).thenThrow(new ValidacionNoDisponibleException());
        mvc(false).perform(post("/vote").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"error\":\"validation_unavailable\"}", true));
        verify(publicador, never()).publicar(any());
    }

    @Test
    @DisplayName("Si la publicación falla responde 503")
    void publicacionFalla() throws Exception {
        when(validador.estaHabilitado(any())).thenReturn(true);
        doThrow(new PublicacionNoDisponibleException()).when(publicador).publicar(any());

        mvc(false).perform(post("/vote").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("validation_unavailable"));
    }

    @Test
    @DisplayName("GET /catalog devuelve candidatos y regiones con los nombres del contrato")
    void catalogo() throws Exception {
        mvc(false).perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"candidates\":[{\"candidateId\":\"" + CANDIDATO
                        + "\",\"name\":\"Lucía Marín Ospina\",\"list\":\"Movimiento Ceiba\"}],"
                        + "\"regions\":[\"norte\",\"sur\"]}", true));
    }

    @Test
    @DisplayName("Una ruta desconocida responde 404 sin cuerpo")
    void rutaDesconocida() throws Exception {
        mvc(false).perform(get("/otra")).andExpect(status().isNotFound()).andExpect(content().string(""));
    }

    @Test
    @DisplayName("Un error interno responde 500 server_error sin detalles")
    void errorInterno() throws Exception {
        when(validador.estaHabilitado(any())).thenThrow(new IllegalStateException("detalle interno"));

        mvc(false).perform(post("/vote").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"error\":\"server_error\"}", true));
    }
}
