package com.zerotrust.votingapi.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Pruebas de dominio puro del Value Object usado por las pruebas de seguridad. */
class DireccionIpTest {

    @Test
    void aceptaIpv4Real() {
        assertEquals("203.0.113.10", new DireccionIp(" 203.0.113.10 ").valor());
    }

    @Test
    void aceptaIpv6() {
        assertEquals("2001:db8::1", new DireccionIp("2001:db8::1").valor());
    }

    @Test
    void rechazaNombreDns() {
        assertThrows(IllegalArgumentException.class, () -> new DireccionIp("example.com"));
    }

    @Test
    void rechazaIpv4FueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> new DireccionIp("999.1.1.1"));
    }

    @Test
    void rechazaVacio() {
        assertThrows(IllegalArgumentException.class, () -> new DireccionIp("   "));
    }
}
