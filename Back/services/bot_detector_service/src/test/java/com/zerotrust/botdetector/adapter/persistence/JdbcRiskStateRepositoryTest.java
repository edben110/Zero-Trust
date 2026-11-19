package com.zerotrust.botdetector.adapter.persistence;

import com.zerotrust.botdetector.exception.ProyeccionNoDisponibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcRiskStateRepositoryTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T12:00:00Z");

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private JdbcRiskStateRepository repositorio;

    @Test
    @DisplayName("Debe hacer UPSERT del estado vigente de la IP")
    void haceUpsert() {
        // Arrange
        Timestamp momento = Timestamp.from(AHORA);

        // Act
        repositorio.actualizar("1.1.1.1", "SOSPECHOSA", 3, 60, AHORA);

        // Assert
        verify(jdbc).update(JdbcRiskStateRepository.UPSERT, "1.1.1.1", "SOSPECHOSA", 3, 60, momento, momento);
    }

    @Test
    @DisplayName("Debe traducir el fallo de la base de datos a ProyeccionNoDisponibleException")
    void traduceElFalloDeLaBaseDeDatos() {
        // Arrange
        when(jdbc.update(anyString(), any(Object[].class))).thenThrow(new DataAccessResourceFailureException("sin conexión"));

        // Act + Assert
        assertThrows(ProyeccionNoDisponibleException.class,
                () -> repositorio.actualizar("1.1.1.1", "NORMAL", 1, 60, AHORA));
    }
}
