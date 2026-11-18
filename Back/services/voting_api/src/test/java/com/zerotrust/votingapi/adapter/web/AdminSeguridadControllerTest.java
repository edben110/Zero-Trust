package com.zerotrust.votingapi.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas de la capa de orquestación con dobles de prueba: sin Kafka ni
 * PostgreSQL reales. Verifican autorización, validación y despacho exacto.
 */
@ExtendWith(MockitoExtension.class)
class AdminSeguridadControllerTest {

    private static final String TOKEN = "0123456789abcdef0123456789abcdef";

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private KafkaTemplate<String, String> kafka;

    private AdminSeguridadController controlador;

    @BeforeEach
    void preparar() {
        controlador = new AdminSeguridadController(TOKEN, jdbc, kafka, new ObjectMapper(),
                "security_drills", 5, 10, 60);
    }

    private void kafkaDisponible() {
        CompletableFuture<SendResult<String, String>> futuro = CompletableFuture.completedFuture(null);
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(futuro);
    }

    @Test
    void rechazaPeticionSinToken() {
        ResponseEntity<?> respuesta = controlador.simular(null, new AdminSeguridadController.SolicitudPrueba("203.0.113.10", 3));
        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        verifyNoInteractions(kafka);
    }

    @Test
    void rechazaIpQueNoEsLiteral() {
        ResponseEntity<?> respuesta = controlador.simular(TOKEN,
                new AdminSeguridadController.SolicitudPrueba("attacker.example.com", 3));
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        verifyNoInteractions(kafka);
    }

    @Test
    void rechazaCantidadFueraDeRango() {
        ResponseEntity<?> respuesta = controlador.simular(TOKEN,
                new AdminSeguridadController.SolicitudPrueba("203.0.113.10", 51));
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        verifyNoInteractions(kafka);
    }

    @Test
    void publicaUnMensajePorCadaVotoSimulado() {
        kafkaDisponible();

        ResponseEntity<?> respuesta = controlador.simular(TOKEN,
                new AdminSeguridadController.SolicitudPrueba("203.0.113.10", 12));

        assertEquals(HttpStatus.ACCEPTED, respuesta.getStatusCode());
        verify(kafka, times(12)).send(eq("security_drills"), eq("203.0.113.10"), anyString());
    }

    @Test
    void elReinicioPublicaLaAccionReset() {
        kafkaDisponible();
        ArgumentCaptor<String> carga = ArgumentCaptor.forClass(String.class);

        ResponseEntity<?> respuesta = controlador.reiniciar(TOKEN,
                new AdminSeguridadController.SolicitudReinicio("203.0.113.10"));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        verify(kafka).send(eq("security_drills"), eq("203.0.113.10"), carga.capture());
        assertTrue(carga.getValue().contains("\"RESET\""));
    }

    @Test
    void laPurgaExigeTokenYNoBorraSinAutorizacion() {
        ResponseEntity<?> respuesta = controlador.purgar(null);

        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        verify(jdbc, never()).update(anyString());
        verifyNoInteractions(kafka);
    }

    @Test
    void laPurgaReiniciaElDetectorYBorraIncidentesYEstados() {
        kafkaDisponible();
        when(jdbc.update(anyString())).thenReturn(4, 2);
        ArgumentCaptor<String> carga = ArgumentCaptor.forClass(String.class);

        ResponseEntity<?> respuesta = controlador.purgar(TOKEN);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        verify(kafka).send(eq("security_drills"), anyString(), carga.capture());
        assertTrue(carga.getValue().contains("\"RESET_ALL\""));
        verify(jdbc).update("DELETE FROM fraud_incidents");
        verify(jdbc).update("DELETE FROM ip_risk_state");
    }
}
