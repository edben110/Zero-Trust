package com.zerotrust.votingapi.controller;

import com.zerotrust.votingapi.domain.ResultadoRegion;
import com.zerotrust.votingapi.config.RepositorioCatalogo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.zerotrust.votingapi.config.ResolutorIpOrigen;
import com.zerotrust.votingapi.security.DerivadorVoterIdHmac;
import com.zerotrust.votingapi.domain.CandidateId;
import com.zerotrust.votingapi.domain.Candidato;
import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.exception.PublicacionNoDisponibleException;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.exception.ValidacionNoDisponibleException;
import com.zerotrust.votingapi.exception.VotoDuplicadoException;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.domain.VotoEmitido;
import com.zerotrust.votingapi.exception.GlobalExceptionHandler;
import com.zerotrust.votingapi.port.ControlRegionVotante;
import com.zerotrust.votingapi.port.EjecutorTransaccional;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.RegistroParticipacion;
import com.zerotrust.votingapi.port.ValidadorVotante;
import com.zerotrust.votingapi.service.ServicioIdentificacion;
import com.zerotrust.votingapi.service.ServicioVotacion;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

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
    @Mock
    private RegistroParticipacion registro;

    private static final ElectionId ELECCION = new ElectionId("elecciones-2026");
    private static final String CEDULA = "0012345678";

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @BeforeEach
    void capturarLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void soltarLogs() {
        logger.detachAppender(logs);
    }

    private static byte[] clavePrueba() {
        byte[] clave = new byte[32];
        for (int i = 0; i < clave.length; i++) {
            clave[i] = (byte) i;
        }
        return clave;
    }

    private static final class TransaccionDirecta implements EjecutorTransaccional {
        @Override
        public <T> T ejecutar(Supplier<T> accion) {
            return accion.get();
        }
    }

    private String todoLoRegistrado() {
        StringBuilder texto = new StringBuilder();
        logs.list.forEach(e -> texto.append(e.getFormattedMessage()).append(e.getThrowableProxy()));
        return texto.toString();
    }

    private MockMvc mvc(boolean confiarEnCabeceras) {
        return mvc(confiarEnCabeceras, (e, v, r) -> ResultadoRegion.CORRECTA);
    }

    private MockMvc mvc(boolean confiarEnCabeceras, ControlRegionVotante controlRegion) {
        Catalogo catalogo = new Catalogo(
                List.of(new Candidato(new CandidateId(CANDIDATO), "Lucía Marín Ospina", "Movimiento Ceiba"),
                        new Candidato(new CandidateId("3e4f5061-7283-4d94-8ea5-c6d7e8f90a1b"),
                                "Segundo candidato", "Otra lista")),
                List.of(new Region("norte"), new Region("sur")));
        RepositorioCatalogo repositorio = mock(RepositorioCatalogo.class);
        lenient().when(repositorio.actual()).thenReturn(catalogo);
        ServicioVotacion servicio = new ServicioVotacion(validador, publicador, registro, voto -> { }, controlRegion,
                (tipo, ip) -> { }, new TransaccionDirecta(), ELECCION,
                Clock.fixed(Instant.parse("2026-11-14T15:00:00Z"), ZoneOffset.UTC), () -> VOTE_ID);
        VotoController votos = new VotoController(servicio, repositorio,
                ResolutorIpOrigen.desde(String.valueOf(confiarEnCabeceras)));
        ConsultaVoterIdController consulta = new ConsultaVoterIdController(
                new ServicioIdentificacion(new DerivadorVoterIdHmac(clavePrueba()), validador, ELECCION));
        return MockMvcBuilders.standaloneSetup(votos, consulta, new CatalogoController(repositorio))
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
    @DisplayName("Un voterId que ya votó en la elección responde 409 con el mensaje y no publica")
    void votoDuplicado() throws Exception {
        when(validador.estaHabilitado(any())).thenReturn(true);
        doThrow(new VotoDuplicadoException()).when(registro).registrar(any(), any());

        mvc(false).perform(post("/vote").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"error\":\"already_voted\","
                        + "\"message\":\"Ya se registró un voto para este documento en esta elección\"}", true));

        verify(publicador, never()).publicar(any());
    }

    @Test
    @DisplayName("Una región distinta de la registrada responde 422 y no publica el voto")
    void regionIncorrecta() throws Exception {
        when(validador.estaHabilitado(any())).thenReturn(true);

        mvc(false, (e, v, r) -> ResultadoRegion.PRIMER_ERROR)
                .perform(post("/vote").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"error\":\"wrong_region\","
                        + "\"message\":\"error esa no es la region que ud designo\"}", true));

        verify(registro, never()).registrar(any(), any());
        verify(publicador, never()).publicar(any());
    }

    @Test
    @DisplayName("Un voto anulado por dos regiones incorrectas responde 409 vote_invalidated")
    void votoAnulado() throws Exception {
        when(validador.estaHabilitado(any())).thenReturn(true);

        mvc(false, (e, v, r) -> ResultadoRegion.ANULADO)
                .perform(post("/vote").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("vote_invalidated"));

        verify(registro, never()).registrar(any(), any());
        verify(publicador, never()).publicar(any());
    }

    @Test
    @DisplayName("Si la base de datos falla responde 503 sin detalles y sin publicar")
    void baseDeDatosCaida() throws Exception {
        when(validador.estaHabilitado(any())).thenReturn(true);
        doThrow(new DataAccessResourceFailureException("detalle interno")).when(registro).registrar(any(), any());

        mvc(false).perform(post("/vote").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"error\":\"validation_unavailable\"}", true));

        verify(publicador, never()).publicar(any());
        assertThat(todoLoRegistrado()).doesNotContain("detalle interno");
    }

    @Test
    @DisplayName("POST /voter-id de un inscrito devuelve el UUID derivado, sin caché, y no registra la cédula")
    void consultaVoterId() throws Exception {
        when(validador.estaHabilitado(new VoterId("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1"))).thenReturn(true);

        mvc(false).perform(post("/voter-id").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"" + CEDULA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("{\"voterId\":\"fae9cb04-ba2b-87ef-882c-f39dd70dbaa1\"}", true));

        verifyNoInteractions(publicador, registro);
        assertThat(todoLoRegistrado()).doesNotContain(CEDULA);
    }

    @Test
    @DisplayName("POST /voter-id de alguien fuera del padrón responde 404 con el aviso y sin el código")
    void consultaFueraDelPadron() throws Exception {
        when(validador.estaHabilitado(any())).thenReturn(false);

        mvc(false).perform(post("/voter-id").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"" + CEDULA + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"error\":\"voter_not_registered\",\"message\":\"El titular de este "
                        + "documento no se encuentra habilitado en el padrón. Por favor pídale al administrador "
                        + "que lo añada.\"}", true))
                .andExpect(content().string(not(containsString("fae9cb04"))));

        assertThat(todoLoRegistrado()).doesNotContain(CEDULA);
    }

    @Test
    @DisplayName("POST /voter-id responde 503 si no se puede consultar el padrón")
    void consultaSinPadron() throws Exception {
        when(validador.estaHabilitado(any())).thenThrow(new ValidacionNoDisponibleException());

        mvc(false).perform(post("/voter-id").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"" + CEDULA + "\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"error\":\"validation_unavailable\"}", true));
    }

    @Test
    @DisplayName("POST /voter-id da el mismo UUID para dos escrituras equivalentes de la cédula")
    void consultaEquivalentes() throws Exception {
        when(validador.estaHabilitado(any())).thenReturn(true);
        for (String escritura : List.of("0.012.345.678", " 0012345678 ")) {
            mvc(false).perform(post("/voter-id").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cedula\":\"" + escritura + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.voterId").value("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1"));
        }
    }

    @Test
    @DisplayName("POST /voter-id rechaza con 400 una cédula inválida, numérica o ausente sin revelarla")
    void consultaInvalida() throws Exception {
        for (String cuerpo : List.of("{\"cedula\":\"98765x4321\"}", "{\"cedula\":12345678}", "{}")) {
            mvc(false).perform(post("/voter-id").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("{\"error\":\"invalid_request\",\"fields\":[\"cedula\"]}", true))
                    .andExpect(content().string(not(containsString("98765"))));
        }
        mvc(false).perform(post("/voter-id").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        assertThat(todoLoRegistrado()).doesNotContain("98765");
    }

    @Test
    @DisplayName("GET /voter-id no existe: la cédula no puede ir en la URL")
    void consultaPorGet() throws Exception {
        mvc(false).perform(get("/voter-id").param("cedula", CEDULA))
                .andExpect(status().isMethodNotAllowed());
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
                        + "\",\"name\":\"Lucía Marín Ospina\",\"list\":\"Movimiento Ceiba\"},"
                        + "{\"candidateId\":\"3e4f5061-7283-4d94-8ea5-c6d7e8f90a1b\","
                        + "\"name\":\"Segundo candidato\",\"list\":\"Otra lista\"}],"
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
