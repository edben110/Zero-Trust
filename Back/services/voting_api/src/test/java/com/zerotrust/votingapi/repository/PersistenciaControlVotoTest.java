package com.zerotrust.votingapi.repository;

import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.exception.VotoDuplicadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistenciaControlVotoTest {

    private static final ElectionId ELECCION = new ElectionId("elecciones-2026");
    private static final VoterId VOTER = new VoterId("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1");

    @Nested
    @DisplayName("RegistroParticipacionJdbc")
    class Registro {

        @Mock
        private JdbcTemplate jdbc;

        @Test
        @DisplayName("Debe insertar una sola fila con la elección y el UUID, sin otros datos")
        void inserta() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
            ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);
            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);

            new RegistroParticipacionJdbc(jdbc).registrar(ELECCION, VOTER);

            verify(jdbc).update(sql.capture(), parametros.capture());
            assertThat(sql.getValue()).contains("voter_election_usage")
                    .contains("ON CONFLICT (election_id, voter_uuid) DO NOTHING");
            assertThat(parametros.getValue()).containsExactly("elecciones-2026", "fae9cb04-ba2b-87ef-882c-f39dd70dbaa1");
        }

        @Test
        @DisplayName("Si la fila ya existía (0 filas insertadas), debe lanzar VotoDuplicadoException")
        void duplicado() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);

            assertThrows(VotoDuplicadoException.class,
                    () -> new RegistroParticipacionJdbc(jdbc).registrar(ELECCION, VOTER));
        }
    }

    @Nested
    @DisplayName("EjecutorTransaccionalSpring")
    class Transaccion {

        @Mock
        private PlatformTransactionManager gestor;
        @Mock
        private TransactionStatus estado;

        @Test
        @DisplayName("Si la acción termina, debe confirmar y devolver su resultado")
        void confirma() {
            when(gestor.getTransaction(any(TransactionDefinition.class))).thenReturn(estado);

            String resultado = new EjecutorTransaccionalSpring(gestor).ejecutar(() -> "ok");

            assertThat(resultado).isEqualTo("ok");
            verify(gestor).commit(estado);
            verify(gestor, never()).rollback(any());
        }

        @Test
        @DisplayName("Si la acción lanza una excepción, debe revertir y no confirmar")
        void revierte() {
            when(gestor.getTransaction(any(TransactionDefinition.class))).thenReturn(estado);

            assertThrows(IllegalStateException.class, () -> new EjecutorTransaccionalSpring(gestor).ejecutar(() -> {
                throw new IllegalStateException("falla la publicación");
            }));

            verify(gestor).rollback(estado);
            verify(gestor, never()).commit(any());
        }
    }
}
