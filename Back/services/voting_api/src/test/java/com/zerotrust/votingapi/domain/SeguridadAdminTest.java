package com.zerotrust.votingapi.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SeguridadAdminTest {

    @Nested
    @DisplayName("PruebaSeguridad")
    class Prueba {

        @Test
        @DisplayName("Debe tomar un voto si no se indica la cantidad")
        void unVotoPorDefecto() {
            // Act
            PruebaSeguridad prueba = PruebaSeguridad.de("203.0.113.10", null);

            // Assert
            assertEquals(new PruebaSeguridad(new DireccionIp("203.0.113.10"), 1), prueba);
        }

        @Test
        @DisplayName("Debe rechazar 0 y 51 votos con el mensaje para el panel")
        void rechazaFueraDeRango() {
            // Act
            SolicitudSeguridadInvalidaException cero = assertThrows(SolicitudSeguridadInvalidaException.class,
                    () -> PruebaSeguridad.de("203.0.113.10", 0));
            SolicitudSeguridadInvalidaException exceso = assertThrows(SolicitudSeguridadInvalidaException.class,
                    () -> PruebaSeguridad.de("203.0.113.10", 51));

            // Assert
            assertEquals("El número de votos debe estar entre 1 y 50.", cero.getMessage());
            assertEquals("El número de votos debe estar entre 1 y 50.", exceso.getMessage());
        }

        @Test
        @DisplayName("Debe aceptar el máximo de 50 votos")
        void aceptaElMaximo() {
            // Act
            PruebaSeguridad prueba = PruebaSeguridad.de("203.0.113.10", 50);

            // Assert
            assertEquals(50, prueba.votos());
        }

        @Test
        @DisplayName("Debe rechazar una IP que no es literal con SolicitudSeguridadInvalidaException")
        void rechazaIpInvalida() {
            // Act + Assert
            assertThrows(SolicitudSeguridadInvalidaException.class, () -> PruebaSeguridad.de("example.com", 1));
        }

        @Test
        @DisplayName("Debe rechazar una IP nula")
        void rechazaIpNula() {
            // Act + Assert
            assertThrows(SolicitudSeguridadInvalidaException.class, () -> new PruebaSeguridad(null, 1));
        }
    }

    @Nested
    @DisplayName("UmbralesSeguridad")
    class Umbrales {

        @Test
        @DisplayName("Debe rechazar umbrales incoherentes")
        void rechazaIncoherentes() {
            // Act + Assert
            assertThrows(IllegalArgumentException.class, () -> new UmbralesSeguridad(0, 10, 60));
            assertThrows(IllegalArgumentException.class, () -> new UmbralesSeguridad(10, 10, 60));
            assertThrows(IllegalArgumentException.class, () -> new UmbralesSeguridad(5, 10, 0));
        }
    }

    @Nested
    @DisplayName("Copias defensivas")
    class Copias {

        @Test
        @DisplayName("ResumenSeguridad debe conservar el orden de niveles y no exponer el mapa recibido")
        void resumenInmutable() {
            // Arrange
            Map<String, Long> riesgo = new LinkedHashMap<>();
            riesgo.put("NORMAL", 1L);
            riesgo.put("BLOQUEADA", 2L);

            // Act
            ResumenSeguridad resumen = new ResumenSeguridad(0, 0, 0, 0, 0, 0, riesgo);
            riesgo.put("SOSPECHOSA", 3L);

            // Assert
            assertEquals(List.of("NORMAL", "BLOQUEADA"), List.copyOf(resumen.riesgoActual().keySet()));
            assertThrows(UnsupportedOperationException.class, () -> resumen.riesgoActual().put("X", 1L));
        }

        @Test
        @DisplayName("PanelSeguridad no debe exponer las listas recibidas")
        void panelInmutable() {
            // Arrange
            List<EstadoIp> estados = new java.util.ArrayList<>();
            ResumenSeguridad resumen = new ResumenSeguridad(0, 0, 0, 0, 0, 0, new HashMap<>());

            // Act
            PanelSeguridad panel = new PanelSeguridad(new UmbralesSeguridad(5, 10, 60), resumen, estados, List.of());
            estados.add(new EstadoIp("1.1.1.1", "NORMAL", 1, 60, null, null));

            // Assert
            assertEquals(0, panel.estados().size());
        }
    }
}
