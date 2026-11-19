package com.zerotrust.votingapi.repository;

import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.ResultadoRegion;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.exception.VotanteNoHabilitadoException;
import com.zerotrust.votingapi.exception.VotoDuplicadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlRegionVotanteJdbcTest {

    private static final String VOTANTE = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final ElectionId ELECCION = new ElectionId("elecciones-2026");
    private static final UUID JORNADA = UUID.fromString("7d1e2f3a-4b5c-4d6e-8f70-112233445566");
    private static final Region NORTE = new Region("Norte");
    private static final Region SUR = new Region("Sur");

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private ControlRegionVotanteJdbc control;

    @SuppressWarnings("unchecked")
    private void votante(List<String> regiones, boolean yaVoto, List<Integer> fallos) {
        when(jdbc.queryForObject(ControlRegionVotanteJdbc.JORNADA_ACTIVA, UUID.class)).thenReturn(JORNADA);
        when(jdbc.query(eq(ControlRegionVotanteJdbc.REGION_VOTANTE), any(RowMapper.class), eq(VOTANTE)))
                .thenReturn(regiones);
        if (regiones.isEmpty()) {
            return;
        }
        when(jdbc.queryForObject(ControlRegionVotanteJdbc.YA_VOTO, Boolean.class, ELECCION.valor(), VOTANTE))
                .thenReturn(yaVoto);
        when(jdbc.query(eq(ControlRegionVotanteJdbc.FALLOS_REGION), any(RowMapper.class), eq(JORNADA), eq(VOTANTE)))
                .thenReturn(fallos);
    }

    private ResultadoRegion evaluar(Region seleccionada) {
        return control.evaluar(ELECCION, new VoterId(VOTANTE), seleccionada);
    }

    @Test
    @DisplayName("Debe bloquear la jornada con FOR SHARE y al votante con FOR UPDATE")
    void conservaLosBloqueos() {
        // Assert
        assertTrue(ControlRegionVotanteJdbc.JORNADA_ACTIVA.endsWith("FOR SHARE"));
        assertTrue(ControlRegionVotanteJdbc.REGION_VOTANTE.endsWith("FOR UPDATE"));
    }

    @Test
    @DisplayName("Debe fallar si no hay elección activa")
    void sinEleccionActiva() {
        // Arrange
        when(jdbc.queryForObject(ControlRegionVotanteJdbc.JORNADA_ACTIVA, UUID.class)).thenReturn(null);

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> evaluar(NORTE));
    }

    @Test
    @DisplayName("Debe lanzar VotanteNoHabilitadoException si el votante no está en el padrón")
    void votanteInexistente() {
        // Arrange
        votante(List.of(), false, List.of());

        // Act + Assert
        assertThrows(VotanteNoHabilitadoException.class, () -> evaluar(NORTE));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("Debe propagar VotoDuplicadoException sin guardar fallos")
    void yaVoto() {
        // Arrange
        votante(List.of("Norte"), true, List.of());

        // Act + Assert
        assertThrows(VotoDuplicadoException.class, () -> evaluar(SUR));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("Debe devolver CORRECTA sin escribir si la región coincide")
    void regionCorrecta() {
        // Arrange
        votante(List.of("Norte"), false, List.of());

        // Act
        ResultadoRegion resultado = evaluar(NORTE);

        // Assert
        assertEquals(ResultadoRegion.CORRECTA, resultado);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("Debe guardar 1 fallo y devolver PRIMER_ERROR en el primer error")
    void primerError() {
        // Arrange
        votante(List.of("Norte"), false, List.of());

        // Act
        ResultadoRegion resultado = evaluar(SUR);

        // Assert
        assertEquals(ResultadoRegion.PRIMER_ERROR, resultado);
        verify(jdbc).update(ControlRegionVotanteJdbc.GUARDAR_FALLOS, JORNADA, VOTANTE, 1);
    }

    @Test
    @DisplayName("Debe guardar 2 fallos y devolver ANULADO en el segundo error")
    void segundoError() {
        // Arrange
        votante(List.of("Norte"), false, List.of(1));

        // Act
        ResultadoRegion resultado = evaluar(SUR);

        // Assert
        assertEquals(ResultadoRegion.ANULADO, resultado);
        verify(jdbc).update(ControlRegionVotanteJdbc.GUARDAR_FALLOS, JORNADA, VOTANTE, 2);
    }

    @Test
    @DisplayName("Debe devolver ANULADO sin escribir si los fallos ya estaban agotados")
    void yaAnulado() {
        // Arrange
        votante(List.of("Norte"), false, List.of(2));

        // Act
        ResultadoRegion resultado = evaluar(NORTE);

        // Assert
        assertEquals(ResultadoRegion.ANULADO, resultado);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
