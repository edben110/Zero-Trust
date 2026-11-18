package com.zerotrust.botdetector.domain;

import com.zerotrust.botdetector.domain.model.IpTrafficContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de dominio puro. Sin Spring, sin Kafka, sin BD.
 * Verifica las transiciones de estado del patrón State y la ventana de tiempo.
 */
class IpTrafficContextTest {

    private static final Instant T0 = Instant.parse("2026-11-18T12:00:00Z");

    private IpTrafficContext context(String ip) {
        // umbral sospechosa: 3, umbral bloqueada: 6, ventana: 60s
        return new IpTrafficContext(ip, 3, 6, 60, T0);
    }

    @Test
    void estadoInicialEsNormal() {
        IpTrafficContext ctx = context("10.0.0.1");
        assertEquals("NORMAL", ctx.riskLevel());
        assertEquals(0, ctx.voteCount());
    }

    @Test
    void noGeneraAlertaEnEstadoNormalBajoUmbral() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.1");

        // Act & Assert: 3 votos, aún no supera el umbral
        for (int i = 0; i < 3; i++) {
            assertFalse(ctx.registerVote(T0.plusSeconds(i)));
        }
        assertEquals("NORMAL", ctx.riskLevel());
    }

    @Test
    void transicionaNormalASospechosaYAlertaAlSuperarUmbral() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.2");

        // Act: 4 votos superan el umbral (3)
        for (int i = 0; i < 3; i++) {
            assertFalse(ctx.registerVote(T0.plusSeconds(i)));
        }
        boolean alert = ctx.registerVote(T0.plusSeconds(3));

        // Assert: la transición misma ya es un indicio y genera alerta
        assertTrue(alert);
        assertEquals("SOSPECHOSA", ctx.riskLevel());
        assertEquals(4, ctx.voteCount());
    }

    @Test
    void generaAlertaEnCadaVotoEstadoSospechosa() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.3");
        for (int i = 0; i < 4; i++) ctx.registerVote(T0.plusSeconds(i)); // lleva a SOSPECHOSA

        // Act
        boolean alert = ctx.registerVote(T0.plusSeconds(4)); // 5to, ya sospechosa

        // Assert
        assertTrue(alert);
        assertEquals("SOSPECHOSA", ctx.riskLevel());
    }

    @Test
    void transicionaSospechosaABloqueadaAlSuperarUmbralCritico() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.4");
        for (int i = 0; i < 6; i++) ctx.registerVote(T0.plusSeconds(i)); // 6 votos → umbral bloqueada

        // Act
        boolean alert = ctx.registerVote(T0.plusSeconds(6)); // 7mo → BLOQUEADA

        // Assert
        assertTrue(alert);
        assertEquals("BLOQUEADA", ctx.riskLevel());
    }

    @Test
    void permaneceBloqueadaYSigueGenerandoAlertas() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.5");
        for (int i = 0; i < 7; i++) ctx.registerVote(T0.plusSeconds(i)); // lleva a BLOQUEADA

        // Act
        boolean alert = ctx.registerVote(T0.plusSeconds(7)); // 8vo, ya bloqueada

        // Assert
        assertTrue(alert);
        assertEquals("BLOQUEADA", ctx.riskLevel());
    }

    @Test
    void laVentanaExpiradaDevuelveNormalAlaParDeVotosAislados() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.6");
        for (int i = 0; i < 4; i++) ctx.registerVote(T0.plusSeconds(i)); // SOSPECHOSA
        assertEquals("SOSPECHOSA", ctx.riskLevel());

        // Act: un voto después de la ventana de 60 s
        boolean alert = ctx.registerVote(T0.plusSeconds(61));

        // Assert: el contador arranca de cero y no alerta
        assertFalse(alert);
        assertEquals("NORMAL", ctx.riskLevel());
        assertEquals(1, ctx.voteCount());
    }

    @Test
    void unaIpBloqueadaNoSeLiberaConLaVentana() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.7");
        for (int i = 0; i < 7; i++) ctx.registerVote(T0.plusSeconds(i)); // BLOQUEADA

        // Act: pasa más de una ventana y sigue votando
        boolean alert = ctx.registerVote(T0.plusSeconds(120));

        // Assert: solo una intervención administrativa la libera
        assertTrue(alert);
        assertEquals("BLOQUEADA", ctx.riskLevel());
    }

    @Test
    void ipNulaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new IpTrafficContext(null, 3, 6, 60, T0));
    }

    @Test
    void umbralBloqueadaMenorQueSospechosaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new IpTrafficContext("10.0.0.1", 10, 5, 60, T0));
    }

    @Test
    void ventanaNoPositivaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new IpTrafficContext("10.0.0.1", 3, 6, 0, T0));
    }
}
