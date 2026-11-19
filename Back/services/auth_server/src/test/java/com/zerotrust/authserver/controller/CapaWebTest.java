package com.zerotrust.authserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.authserver.domain.Audiencia;
import com.zerotrust.authserver.domain.ClientId;
import com.zerotrust.authserver.domain.ClienteRegistrado;
import com.zerotrust.authserver.domain.Emisor;
import com.zerotrust.authserver.domain.HashSecreto;
import com.zerotrust.authserver.domain.RegistroClientes;
import com.zerotrust.authserver.domain.Scope;
import com.zerotrust.authserver.domain.SecretoCliente;
import com.zerotrust.authserver.domain.Ttl;
import com.zerotrust.authserver.exception.GlobalExceptionHandler;
import com.zerotrust.authserver.port.FirmadorTokens;
import com.zerotrust.authserver.port.PublicadorClaves;
import com.zerotrust.authserver.port.RegistroEventos;
import com.zerotrust.authserver.port.VerificadorSecretos;
import com.zerotrust.authserver.service.ServicioEmisionToken;
import com.zerotrust.authserver.service.ServicioJwks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CapaWebTest {

    private static final HashSecreto HASH_REAL = new HashSecreto("$2b$12$" + "r".repeat(53));
    private static final HashSecreto HASH_FICTICIO = new HashSecreto("$2b$12$" + "f".repeat(53));
    private static final ClientId VOTING_API = new ClientId("voting_api");

    @Mock
    private VerificadorSecretos verificador;
    @Mock
    private FirmadorTokens firmador;
    @Mock
    private PublicadorClaves publicador;
    @Mock
    private RegistroEventos eventos;

    private MockMvc mvc;

    @BeforeEach
    void construir() {
        RegistroClientes registro = new RegistroClientes(List.of(new ClienteRegistrado(VOTING_API, HASH_REAL,
                List.of(new Scope("a")), List.of(Audiencia.RABBITMQ, Audiencia.KAFKA))));
        ServicioEmisionToken servicio = new ServicioEmisionToken(registro, verificador, firmador,
                new Emisor("https://vm-auth"), Ttl.porDefecto(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        ObjectMapper json = new ObjectMapper();
        TokenController token = new TokenController(new InterpretePeticionToken(), servicio, eventos, json);
        JwksController jwks = new JwksController(new ServicioJwks(publicador), json);
        mvc = MockMvcBuilders.standaloneSetup(token, jwks)
                .setControllerAdvice(new GlobalExceptionHandler(registro, eventos, json))
                .build();
        lenient().when(verificador.hashFicticio()).thenReturn(HASH_FICTICIO);
        lenient().when(verificador.coincide(new SecretoCliente("correcto"), HASH_REAL)).thenReturn(true);
        lenient().when(firmador.firmar(any())).thenReturn("jwt.firmado.aqui");
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }

    private ResultActions pedirToken(String cuerpo, String... autorizacion) throws Exception {
        var peticion = post("/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(cuerpo.getBytes(StandardCharsets.UTF_8));
        for (String valor : autorizacion) {
            peticion.header(HttpHeaders.AUTHORIZATION, valor);
        }
        return mvc.perform(peticion);
    }

    private static void cabecerasDeError(ResultActions resultado) throws Exception {
        resultado.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"));
    }

    @Nested
    @DisplayName("POST /token")
    class Token {

        @Test
        @DisplayName("Debe responder 200 con JSON snake_case, las tres cabeceras y una línea de log")
        void emite() throws Exception {
            pedirToken("grant_type=client_credentials", basic("voting_api:correcto"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"))
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                    .andExpect(jsonPath("$.access_token").value("jwt.firmado.aqui"))
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.expires_in").value(300));

            verify(eventos, times(1)).emisionCorrecta(VOTING_API);
            verifyNoMoreInteractions(eventos);
        }

        @Test
        @DisplayName("Debe responder JSON aunque Accept pida XML")
        void ignoraAccept() throws Exception {
            mvc.perform(post("/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .accept(MediaType.APPLICATION_XML)
                            .content("grant_type=client_credentials&client_id=voting_api&client_secret=correcto"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"));
        }

        @Test
        @DisplayName("401 con WWW-Authenticate solo cuando las credenciales llegaron por Basic")
        void invalidClient() throws Exception {
            cabecerasDeError(pedirToken("grant_type=client_credentials", basic("voting_api:malo"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"auth_server\""))
                    .andExpect(content().json("{\"error\":\"invalid_client\"}", true)));

            pedirToken("grant_type=client_credentials&client_id=voting_api&client_secret=malo")
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
        }

        @Test
        @DisplayName("Un rechazo de un cliente registrado registra una línea con su client_id")
        void logRechazoRegistrado() throws Exception {
            pedirToken("grant_type=client_credentials", basic("voting_api:malo"));

            verify(eventos, times(1)).rechazoClienteRegistrado("invalid_client", VOTING_API);
            verifyNoMoreInteractions(eventos);
        }

        @Test
        @DisplayName("Un rechazo de un cliente desconocido registra una línea sin el valor recibido")
        void logRechazoDesconocido() throws Exception {
            pedirToken("grant_type=client_credentials", basic("intruso:malo"));

            verify(eventos, times(1)).rechazoClienteDesconocido("invalid_client");
            verifyNoMoreInteractions(eventos);
        }

        @Test
        @DisplayName("Un invalid_request también registra exactamente una línea, con el client_id si está registrado")
        void logInvalidRequest() throws Exception {
            cabecerasDeError(pedirToken("client_id=voting_api&client_secret=correcto")
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("{\"error\":\"invalid_request\"}", true)));

            verify(eventos, times(1)).rechazoClienteRegistrado("invalid_request", VOTING_API);
            verifyNoMoreInteractions(eventos);
        }

        @Test
        @DisplayName("Las credenciales en la query string se ignoran")
        void ignoraQueryString() throws Exception {
            mvc.perform(post("/token?client_id=voting_api&client_secret=correcto")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .content("grant_type=client_credentials"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("{\"error\":\"invalid_request\"}", true));
        }

        @Test
        @DisplayName("Orden de RF-24 con el servicio real: grant antes que cliente, cliente antes que scope")
        void ordenRf24() throws Exception {
            pedirToken("grant_type=password", basic("intruso:malo"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("{\"error\":\"unsupported_grant_type\"}", true));
            pedirToken("grant_type=client_credentials&scope=z", basic("voting_api:malo"))
                    .andExpect(status().isUnauthorized());
            pedirToken("grant_type=client_credentials&scope=a%22b", basic("voting_api:malo"))
                    .andExpect(status().isUnauthorized());
            pedirToken("grant_type=client_credentials&scope=z", basic("voting_api:correcto"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("{\"error\":\"invalid_scope\"}", true));
            mvc.perform(post("/token").contentType(MediaType.APPLICATION_JSON).content("grant_type=password"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("{\"error\":\"invalid_request\"}", true));
        }

        @Test
        @DisplayName("Un error interno responde 500 server_error con las cabeceras de error")
        void errorInterno() throws Exception {
            when(firmador.firmar(any())).thenThrow(new IllegalStateException("fallo interno con detalle"));

            cabecerasDeError(pedirToken("grant_type=client_credentials", basic("voting_api:correcto"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().json("{\"error\":\"server_error\"}", true)));
            verify(eventos, times(1)).rechazoClienteDesconocido("server_error");
        }
    }

    @Nested
    @DisplayName("Métodos y rutas")
    class MetodosYRutas {

        @Test
        @DisplayName("GET /token responde 405 con Allow: POST, cuerpo de error y una línea de log")
        void getToken() throws Exception {
            cabecerasDeError(mvc.perform(get("/token"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(header().string(HttpHeaders.ALLOW, "POST"))
                    .andExpect(content().json("{\"error\":\"invalid_request\"}", true)));

            verify(eventos, times(1)).rechazoClienteDesconocido("invalid_request");
            verifyNoMoreInteractions(eventos);
        }

        @Test
        @DisplayName("HEAD /token responde 405 con Allow: POST y sin cuerpo")
        void headToken() throws Exception {
            mvc.perform(head("/token"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(header().string(HttpHeaders.ALLOW, "POST"))
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("POST al JWKS responde 405 con Allow: GET y no registra log")
        void postJwks() throws Exception {
            cabecerasDeError(mvc.perform(post("/.well-known/jwks.json"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(header().string(HttpHeaders.ALLOW, "GET"))
                    .andExpect(content().json("{\"error\":\"invalid_request\"}", true)));

            verifyNoInteractions(eventos);
        }

        @Test
        @DisplayName("HEAD al JWKS responde 200 sin cuerpo")
        void headJwks() throws Exception {
            when(publicador.jwkSetPublico()).thenReturn(Map.of("keys", List.of()));

            mvc.perform(head("/.well-known/jwks.json"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("OPTIONS responde 200 con Allow en las dos rutas y no registra log")
        void optionsEnAmbas() throws Exception {
            mvc.perform(options("/token"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ALLOW, org.hamcrest.Matchers.containsString("POST")));
            mvc.perform(options("/.well-known/jwks.json"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ALLOW, org.hamcrest.Matchers.containsString("GET")));

            verifyNoInteractions(eventos);
        }

        @Test
        @DisplayName("Una ruta desconocida, incluida /error, responde 404 sin cuerpo")
        void rutaDesconocida() throws Exception {
            mvc.perform(get("/otra")).andExpect(status().isNotFound()).andExpect(content().string(""));
            mvc.perform(get("/error")).andExpect(status().isNotFound()).andExpect(content().string(""));
        }

        @Test
        @DisplayName("GET al JWKS responde 200 JSON aunque Accept pida XML")
        void getJwks() throws Exception {
            when(publicador.jwkSetPublico()).thenReturn(Map.of("keys", List.of(Map.of("kid", "clave-1"))));

            mvc.perform(get("/.well-known/jwks.json").accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/json"))
                    .andExpect(jsonPath("$.keys[0].kid").value("clave-1"));
            verify(publicador).jwkSetPublico();
            verifyNoInteractions(eventos);
        }
    }

    @Test
    @DisplayName("El controlador de token no registra nada más allá de su línea")
    void unaSolaLinea() throws Exception {
        pedirToken("grant_type=client_credentials", basic("voting_api:correcto"));

        verify(eventos, times(1)).emisionCorrecta(eq(VOTING_API));
        verifyNoMoreInteractions(eventos);
    }
}
