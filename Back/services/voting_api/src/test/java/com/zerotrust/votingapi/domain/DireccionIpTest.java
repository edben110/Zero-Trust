package com.zerotrust.votingapi.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Pruebas de dominio puro del Value Object usado por las pruebas de seguridad. */
class DireccionIpTest {

    @Test
    @DisplayName("Debe aceptar una IPv4 y quitar los espacios")
    void aceptaIpv4Real() {
        // Act
        DireccionIp ip = new DireccionIp(" 203.0.113.10 ");

        // Assert
        assertEquals("203.0.113.10", ip.valor());
    }

    @Test
    @DisplayName("Debe aceptar una IPv6")
    void aceptaIpv6() {
        // Act
        DireccionIp ip = new DireccionIp("2001:db8::1");

        // Assert
        assertEquals("2001:db8::1", ip.valor());
    }

    @Test
    @DisplayName("Debe rechazar un nombre DNS con el mensaje para el panel")
    void rechazaNombreDns() {
        // Act
        SolicitudSeguridadInvalidaException error = assertThrows(SolicitudSeguridadInvalidaException.class,
                () -> new DireccionIp("example.com"));

        // Assert
        assertEquals("La IP no es un literal IPv4 o IPv6 válido.", error.getMessage());
    }

    @Test
    @DisplayName("Debe rechazar una IPv4 fuera de rango")
    void rechazaIpv4FueraDeRango() {
        // Act + Assert
        assertThrows(SolicitudSeguridadInvalidaException.class, () -> new DireccionIp("999.1.1.1"));
    }

    @Test
    @DisplayName("Debe rechazar una IP vacía como IllegalArgumentException")
    void rechazaVacio() {
        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new DireccionIp("   "));

        // Assert
        assertEquals("La IP es obligatoria.", error.getMessage());
    }
}
