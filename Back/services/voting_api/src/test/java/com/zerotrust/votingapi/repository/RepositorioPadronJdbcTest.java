package com.zerotrust.votingapi.repository;

import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.VotanteInscrito;
import com.zerotrust.votingapi.exception.VotanteYaInscritoException;
import com.zerotrust.votingapi.domain.VoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepositorioPadronJdbc")
class RepositorioPadronJdbcTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T10:00:00Z");
    private static final VotanteInscrito VOTANTE = new VotanteInscrito(
            new VoterId("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1"), new Region("norte"),
            AHORA);

    @Mock
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Debe insertar en registered_voters el UUID, la región y la fecha, sin datos personales")
    void inserta() {
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);

        new RepositorioPadronJdbc(jdbc).inscribir(VOTANTE);

        verify(jdbc).update(sql.capture(), parametros.capture());
        assertThat(sql.getValue()).contains("registered_voters").contains("ON CONFLICT (voter_id) DO NOTHING");
        assertThat(parametros.getValue()).containsExactly("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1",
                "norte", Timestamp.from(AHORA));
    }

    @Test
    @DisplayName("Si el voterId ya existía (0 filas), debe lanzar VotanteYaInscritoException")
    void yaInscrito() {
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);

        assertThrows(VotanteYaInscritoException.class, () -> new RepositorioPadronJdbc(jdbc).inscribir(VOTANTE));
    }
}
