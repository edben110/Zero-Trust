package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.domain.state.IpTrafficContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class RegistroContextosIpTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T12:00:00Z");

    private final RegistroContextosIp registro = new RegistroContextosIp(2, 4, 60);

    @Test
    @DisplayName("Debe crear el contexto de una IP una sola vez y con los umbrales configurados")
    void creaUnaVez() {
        // Act
        IpTrafficContext primero = registro.contextoDe("1.1.1.1", AHORA);
        IpTrafficContext segundo = registro.contextoDe("1.1.1.1", AHORA.plusSeconds(5));

        // Assert
        assertSame(primero, segundo);
        assertEquals(2, primero.suspiciousThreshold());
        assertEquals(4, primero.blockedThreshold());
        assertEquals(60, primero.windowSeconds());
    }

    @Test
    @DisplayName("Una IP sin contexto debe verse NORMAL y con 0 votos")
    void ipDesconocida() {
        // Act + Assert
        assertEquals("NORMAL", registro.nivelRiesgo("9.9.9.9"));
        assertEquals(0, registro.votos("9.9.9.9"));
    }

    @Test
    @DisplayName("Debe reflejar el nivel y los votos del contexto vigente")
    void reflejaElContexto() {
        // Arrange
        IpTrafficContext contexto = registro.contextoDe("2.2.2.2", AHORA);

        // Act
        contexto.registerVote(AHORA);
        contexto.registerVote(AHORA);
        contexto.registerVote(AHORA);

        // Assert
        assertEquals("SOSPECHOSA", registro.nivelRiesgo("2.2.2.2"));
        assertEquals(3, registro.votos("2.2.2.2"));
    }

    @Test
    @DisplayName("Olvidar una IP debe crear un contexto nuevo en el siguiente voto sin tocar las demás")
    void olvidaUnaIp() {
        // Arrange
        IpTrafficContext anterior = registro.contextoDe("3.3.3.3", AHORA);
        IpTrafficContext otra = registro.contextoDe("4.4.4.4", AHORA);

        // Act
        registro.olvidar("3.3.3.3");

        // Assert
        assertNotSame(anterior, registro.contextoDe("3.3.3.3", AHORA));
        assertSame(otra, registro.contextoDe("4.4.4.4", AHORA));
    }

    @Test
    @DisplayName("Olvidar todas debe vaciar el registro")
    void olvidaTodas() {
        // Arrange
        registro.contextoDe("5.5.5.5", AHORA).registerVote(AHORA);

        // Act
        registro.olvidarTodos();

        // Assert
        assertEquals(0, registro.votos("5.5.5.5"));
    }
}
