package com.zerotrust.votingapi.domain;

import com.zerotrust.votingapi.exception.VotoDuplicadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VotanteEnJornadaTest {

    private static final Region NORTE = new Region("Norte");
    private static final Region SUR = new Region("Sur");

    @Nested
    @DisplayName("Construcción")
    class Construccion {

        @Test
        @DisplayName("Debe rechazar una región registrada nula")
        void rechazaRegionNula() {
            // Act + Assert
            assertThrows(IllegalArgumentException.class, () -> new VotanteEnJornada(null, false, 0));
        }

        @Test
        @DisplayName("Debe rechazar un número de fallos negativo")
        void rechazaFallosNegativos() {
            // Act + Assert
            assertThrows(IllegalArgumentException.class, () -> new VotanteEnJornada(NORTE, false, -1));
        }
    }

    @Nested
    @DisplayName("Voto duplicado")
    class VotoDuplicado {

        @Test
        @DisplayName("Debe lanzar VotoDuplicadoException si ya votó, aunque la región coincida")
        void yaVotoConRegionCorrecta() {
            // Arrange
            VotanteEnJornada votante = new VotanteEnJornada(NORTE, true, 0);

            // Act + Assert
            assertThrows(VotoDuplicadoException.class, () -> votante.evaluarRegion(NORTE));
        }

        @Test
        @DisplayName("Debe priorizar el voto duplicado sobre la anulación por fallos")
        void yaVotoConFallosAgotados() {
            // Arrange
            VotanteEnJornada votante = new VotanteEnJornada(NORTE, true, VotanteEnJornada.MAX_FALLOS_REGION);

            // Act + Assert
            assertThrows(VotoDuplicadoException.class, () -> votante.evaluarRegion(SUR));
        }
    }

    @Nested
    @DisplayName("Sin fallos previos")
    class SinFallos {

        @Test
        @DisplayName("Debe aceptar la región registrada sin sumar fallos")
        void regionCorrecta() {
            // Arrange
            VotanteEnJornada votante = new VotanteEnJornada(NORTE, false, 0);

            // Act
            EvaluacionRegion evaluacion = votante.evaluarRegion(NORTE);

            // Assert
            assertEquals(new EvaluacionRegion(ResultadoRegion.CORRECTA, 0), evaluacion);
            assertFalse(evaluacion.fallosCambiaron(0));
        }

        @Test
        @DisplayName("Debe marcar el primer error y subir los fallos a 1")
        void primerError() {
            // Arrange
            VotanteEnJornada votante = new VotanteEnJornada(NORTE, false, 0);

            // Act
            EvaluacionRegion evaluacion = votante.evaluarRegion(SUR);

            // Assert
            assertEquals(new EvaluacionRegion(ResultadoRegion.PRIMER_ERROR, 1), evaluacion);
            assertTrue(evaluacion.fallosCambiaron(0));
        }
    }

    @Nested
    @DisplayName("Con un fallo previo")
    class UnFallo {

        @Test
        @DisplayName("Debe aceptar la región correcta y conservar el fallo previo")
        void regionCorrecta() {
            // Arrange
            VotanteEnJornada votante = new VotanteEnJornada(NORTE, false, 1);

            // Act
            EvaluacionRegion evaluacion = votante.evaluarRegion(NORTE);

            // Assert
            assertEquals(new EvaluacionRegion(ResultadoRegion.CORRECTA, 1), evaluacion);
        }

        @Test
        @DisplayName("Debe anular el voto en el segundo error y subir los fallos a 2")
        void segundoErrorAnula() {
            // Arrange
            VotanteEnJornada votante = new VotanteEnJornada(NORTE, false, 1);

            // Act
            EvaluacionRegion evaluacion = votante.evaluarRegion(SUR);

            // Assert
            assertEquals(new EvaluacionRegion(ResultadoRegion.ANULADO, VotanteEnJornada.MAX_FALLOS_REGION), evaluacion);
            assertTrue(evaluacion.fallosCambiaron(1));
        }
    }

    @Nested
    @DisplayName("Con los fallos agotados")
    class FallosAgotados {

        @Test
        @DisplayName("Debe seguir anulado aunque elija la región correcta")
        void anuladoConRegionCorrecta() {
            // Arrange
            VotanteEnJornada votante = new VotanteEnJornada(NORTE, false, 2);

            // Act
            EvaluacionRegion evaluacion = votante.evaluarRegion(NORTE);

            // Assert
            assertEquals(new EvaluacionRegion(ResultadoRegion.ANULADO, 2), evaluacion);
            assertFalse(evaluacion.fallosCambiaron(2));
        }

        @Test
        @DisplayName("No debe superar el tope de 2 fallos con otra región incorrecta")
        void topeDeFallos() {
            // Arrange
            VotanteEnJornada votante = new VotanteEnJornada(NORTE, false, 2);

            // Act
            EvaluacionRegion evaluacion = votante.evaluarRegion(SUR);

            // Assert
            assertEquals(new EvaluacionRegion(ResultadoRegion.ANULADO, 2), evaluacion);
            assertFalse(evaluacion.fallosCambiaron(2));
        }

        @Test
        @DisplayName("Debe tratar más de 2 fallos guardados como anulado sin cambiarlos")
        void masDelTope() {
            // Arrange
            VotanteEnJornada votante = new VotanteEnJornada(NORTE, false, 3);

            // Act
            EvaluacionRegion evaluacion = votante.evaluarRegion(SUR);

            // Assert
            assertEquals(new EvaluacionRegion(ResultadoRegion.ANULADO, 3), evaluacion);
            assertFalse(evaluacion.fallosCambiaron(3));
        }
    }

    @Test
    @DisplayName("El tope de fallos de región debe ser 2")
    void topeEsDos() {
        // Assert
        assertEquals(2, VotanteEnJornada.MAX_FALLOS_REGION);
    }
}
