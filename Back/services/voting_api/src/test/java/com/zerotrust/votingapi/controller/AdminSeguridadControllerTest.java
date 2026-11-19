package com.zerotrust.votingapi.controller;

import com.zerotrust.votingapi.domain.DireccionIp;
import com.zerotrust.votingapi.domain.EstadoIp;
import com.zerotrust.votingapi.domain.IncidenteSeguridad;
import com.zerotrust.votingapi.exception.OperacionSeguridadNoDisponibleException;
import com.zerotrust.votingapi.domain.PanelSeguridad;
import com.zerotrust.votingapi.domain.PruebaSeguridad;
import com.zerotrust.votingapi.domain.ResultadoPurga;
import com.zerotrust.votingapi.domain.ResumenSeguridad;
import com.zerotrust.votingapi.domain.UmbralesSeguridad;
import com.zerotrust.votingapi.exception.GlobalExceptionHandler;
import com.zerotrust.votingapi.service.ServicioSeguridadAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminSeguridadControllerTest {

    private static final String TOKEN = "0123456789abcdef0123456789abcdef";
    private static final String BASE = "/internal/admin/security";
    private static final String IP = "203.0.113.10";

    @Mock
    private ServicioSeguridadAdmin servicio;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new AdminSeguridadController(TOKEN, servicio))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("Debe rechazar un token de administración de menos de 32 caracteres")
    void rechazaTokenCorto() {
        // Arrange
        String corto = "corto";

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> new AdminSeguridadController(corto, servicio));
    }

    @Nested
    @DisplayName("POST /drills")
    class Simulacros {

        @Test
        @DisplayName("Debe responder 401 sin token y no publicar nada")
        void rechazaSinToken() throws Exception {
            // Arrange
            String cuerpo = "{\"ip\":\"" + IP + "\",\"votes\":3}";

            // Act + Assert
            mvc.perform(post(BASE + "/drills").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(servicio);
        }

        @Test
        @DisplayName("Debe responder 400 con el motivo si la IP no es un literal")
        void rechazaIpQueNoEsLiteral() throws Exception {
            // Arrange
            String cuerpo = "{\"ip\":\"attacker.example.com\",\"votes\":3}";

            // Act + Assert
            mvc.perform(post(BASE + "/drills").header("X-Admin-Token", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("La IP no es un literal IPv4 o IPv6 válido."));
            verifyNoInteractions(servicio);
        }

        @Test
        @DisplayName("Debe responder 400 si el número de votos está fuera de 1..50")
        void rechazaCantidadFueraDeRango() throws Exception {
            // Arrange
            String cuerpo = "{\"ip\":\"" + IP + "\",\"votes\":51}";

            // Act + Assert
            mvc.perform(post(BASE + "/drills").header("X-Admin-Token", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("El número de votos debe estar entre 1 y 50."));
            verifyNoInteractions(servicio);
        }

        @Test
        @DisplayName("Debe responder 400 si falta el cuerpo")
        void rechazaSinCuerpo() throws Exception {
            // Act + Assert
            mvc.perform(post(BASE + "/drills").header("X-Admin-Token", TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Indique la IP y el número de votos."));
            verifyNoInteractions(servicio);
        }

        @Test
        @DisplayName("Debe aceptar la prueba y delegarla al servicio")
        void aceptaLaPrueba() throws Exception {
            // Arrange
            when(servicio.destinoComandos()).thenReturn("security_drills");
            String cuerpo = "{\"ip\":\"" + IP + "\",\"votes\":12}";

            // Act + Assert
            mvc.perform(post(BASE + "/drills").header("X-Admin-Token", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isAccepted())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.accepted").value(true))
                    .andExpect(jsonPath("$.ip").value(IP))
                    .andExpect(jsonPath("$.votes").value(12))
                    .andExpect(jsonPath("$.topic").value("security_drills"));
            verify(servicio).simular(new PruebaSeguridad(new DireccionIp(IP), 12));
        }

        @Test
        @DisplayName("Debe tomar un voto si no se indica la cantidad")
        void unVotoPorDefecto() throws Exception {
            // Arrange
            String cuerpo = "{\"ip\":\"" + IP + "\"}";

            // Act + Assert
            mvc.perform(post(BASE + "/drills").header("X-Admin-Token", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isAccepted());
            verify(servicio).simular(new PruebaSeguridad(new DireccionIp(IP), 1));
        }

        @Test
        @DisplayName("Debe responder 503 con el mensaje del servicio si Kafka no confirma")
        void kafkaNoDisponible() throws Exception {
            // Arrange
            doThrow(new OperacionSeguridadNoDisponibleException("No se pudo publicar la prueba en Kafka.",
                    new RuntimeException())).when(servicio).simular(new PruebaSeguridad(new DireccionIp(IP), 2));
            String cuerpo = "{\"ip\":\"" + IP + "\",\"votes\":2}";

            // Act + Assert
            mvc.perform(post(BASE + "/drills").header("X-Admin-Token", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("No se pudo publicar la prueba en Kafka."));
        }
    }

    @Nested
    @DisplayName("POST /reset")
    class Reinicio {

        @Test
        @DisplayName("Debe reiniciar la IP indicada")
        void reiniciaLaIp() throws Exception {
            // Arrange
            String cuerpo = "{\"ip\":\"" + IP + "\"}";

            // Act + Assert
            mvc.perform(post(BASE + "/reset").header("X-Admin-Token", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reset").value(true))
                    .andExpect(jsonPath("$.ip").value(IP));
            verify(servicio).reiniciar(new DireccionIp(IP));
        }

        @Test
        @DisplayName("Debe responder 400 si falta la IP")
        void rechazaSinIp() throws Exception {
            // Act + Assert
            mvc.perform(post(BASE + "/reset").header("X-Admin-Token", TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("La IP es obligatoria."));
            verifyNoInteractions(servicio);
        }
    }

    @Nested
    @DisplayName("POST /purge")
    class Purga {

        @Test
        @DisplayName("Debe exigir token y no purgar sin autorización")
        void exigeToken() throws Exception {
            // Act + Assert
            mvc.perform(post(BASE + "/purge")).andExpect(status().isUnauthorized());
            verifyNoInteractions(servicio);
        }

        @Test
        @DisplayName("Debe devolver cuántos incidentes y estados se borraron")
        void purga() throws Exception {
            // Arrange
            when(servicio.purgar()).thenReturn(new ResultadoPurga(4, 2));

            // Act + Assert
            mvc.perform(post(BASE + "/purge").header("X-Admin-Token", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.purged").value(true))
                    .andExpect(jsonPath("$.incidents").value(4))
                    .andExpect(jsonPath("$.ips").value(2));
        }

        @Test
        @DisplayName("Debe responder 503 si el detector no se pudo reiniciar")
        void detectorNoDisponible() throws Exception {
            // Arrange
            when(servicio.purgar()).thenThrow(new OperacionSeguridadNoDisponibleException(
                    "No se pudo reiniciar el detector en Kafka; no se borraron registros.", new RuntimeException()));

            // Act + Assert
            mvc.perform(post(BASE + "/purge").header("X-Admin-Token", TOKEN))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("No se pudo reiniciar el detector en Kafka; no se borraron registros."));
        }
    }

    @Nested
    @DisplayName("GET /incidents")
    class Incidentes {

        @Test
        @DisplayName("Debe devolver umbrales, resumen, estados e incidentes con el formato del panel")
        void devuelveElPanel() throws Exception {
            // Arrange
            Map<String, Long> riesgo = new LinkedHashMap<>();
            riesgo.put("NORMAL", 3L);
            riesgo.put("SOSPECHOSA", 1L);
            riesgo.put("BLOQUEADA", 0L);
            Instant visto = Instant.parse("2026-11-18T12:00:00Z");
            when(servicio.panel()).thenReturn(new PanelSeguridad(
                    new UmbralesSeguridad(5, 10, 60),
                    new ResumenSeguridad(7, 5, 2, 1, 1, 4, riesgo),
                    List.of(new EstadoIp(IP, "SOSPECHOSA", 6, 60, visto, null)),
                    List.of(new IncidenteSeguridad(9, IP, "IP_PATTERN", "SOSPECHOSA", 6, 60, "DRILL", visto))));

            // Act + Assert
            mvc.perform(get(BASE + "/incidents").header("X-Admin-Token", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.thresholds.suspicious").value(5))
                    .andExpect(jsonPath("$.thresholds.blocked").value(10))
                    .andExpect(jsonPath("$.thresholds.windowSeconds").value(60))
                    .andExpect(jsonPath("$.summary.incidents").value(7))
                    .andExpect(jsonPath("$.summary.live").value(5))
                    .andExpect(jsonPath("$.summary.drills").value(2))
                    .andExpect(jsonPath("$.summary.duplicates").value(1))
                    .andExpect(jsonPath("$.summary.wrongRegion").value(1))
                    .andExpect(jsonPath("$.summary.ipsWithIncidents").value(4))
                    .andExpect(jsonPath("$.summary.currentRisk.SOSPECHOSA").value(1))
                    .andExpect(jsonPath("$.ips[0].ip").value(IP))
                    .andExpect(jsonPath("$.ips[0].riskLevel").value("SOSPECHOSA"))
                    .andExpect(jsonPath("$.ips[0].voteCount").value(6))
                    .andExpect(jsonPath("$.ips[0].windowSeconds").value(60))
                    .andExpect(jsonPath("$.ips[0].firstSeen").value("2026-11-18T12:00:00Z"))
                    .andExpect(jsonPath("$.ips[0].lastSeen").value(nullValue()))
                    .andExpect(jsonPath("$.incidents[0].id").value(9))
                    .andExpect(jsonPath("$.incidents[0].kind").value("IP_PATTERN"))
                    .andExpect(jsonPath("$.incidents[0].origin").value("DRILL"))
                    .andExpect(jsonPath("$.incidents[0].detectedAt").value("2026-11-18T12:00:00Z"));
        }

        @Test
        @DisplayName("Debe responder 503 si PostgreSQL no responde")
        void baseDeDatosNoDisponible() throws Exception {
            // Arrange
            when(servicio.panel()).thenThrow(new OperacionSeguridadNoDisponibleException(
                    "No se pudo consultar PostgreSQL.", new RuntimeException()));

            // Act + Assert
            mvc.perform(get(BASE + "/incidents").header("X-Admin-Token", TOKEN))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("No se pudo consultar PostgreSQL."));
        }
    }
}
