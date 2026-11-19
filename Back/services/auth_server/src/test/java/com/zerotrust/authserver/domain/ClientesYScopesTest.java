package com.zerotrust.authserver.domain;

import com.zerotrust.authserver.exception.ScopeNoPermitidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientesYScopesTest {

    static final HashSecreto HASH_12 = new HashSecreto("$2b$12$" + "a".repeat(53));
    static final HashSecreto HASH_10 = new HashSecreto("$2b$10$" + "a".repeat(53));

    static ClienteRegistrado cliente(String id, List<String> scopes, List<Audiencia> audiencias) {
        return new ClienteRegistrado(new ClientId(id), HASH_12, scopes.stream().map(Scope::new).toList(), audiencias);
    }

    @Nested
    @DisplayName("ClienteRegistrado")
    class ClienteRegistradoTest {

        @Test
        @DisplayName("Debe admitir scopes vacíos solo si la audiencia es exactamente kafka")
        void scopesVaciosSoloConKafka() {
            assertThat(cliente("voter_registration_service", List.of(), List.of(Audiencia.KAFKA)).audiencias())
                    .containsExactly(Audiencia.KAFKA);
            assertThrows(IllegalArgumentException.class,
                    () -> cliente("worker", List.of(), List.of(Audiencia.RABBITMQ)));
            assertThrows(IllegalArgumentException.class,
                    () -> cliente("api", List.of(), List.of(Audiencia.KAFKA, Audiencia.RABBITMQ)));
        }

        @Test
        @DisplayName("Debe rechazar scopes repetidos, audiencias vacías o repetidas")
        void rechazaRepetidosYVacios() {
            assertThrows(IllegalArgumentException.class, () -> cliente("a", List.of("x", "x"), List.of(Audiencia.RABBITMQ)));
            assertThrows(IllegalArgumentException.class, () -> cliente("a", List.of("x"), List.of()));
            assertThrows(IllegalArgumentException.class,
                    () -> cliente("a", List.of("x"), List.of(Audiencia.RABBITMQ, Audiencia.RABBITMQ)));
        }

        @Test
        @DisplayName("Debe conservar el orden de las audiencias del archivo y no exponer la lista interna")
        void conservaOrdenAudiencias() {
            ClienteRegistrado cliente = cliente("api", List.of("x"), List.of(Audiencia.RABBITMQ, Audiencia.KAFKA));

            assertThat(cliente.audiencias()).containsExactly(Audiencia.RABBITMQ, Audiencia.KAFKA);
            assertThrows(UnsupportedOperationException.class, () -> cliente.audiencias().add(Audiencia.KAFKA));
        }

        @Test
        @DisplayName("Sin scope pedido, debe conceder todos los asignados en el orden del archivo")
        void sinScopeConcedeTodos() {
            ClienteRegistrado cliente = cliente("api", List.of("a", "b", "c"), List.of(Audiencia.RABBITMQ));

            for (String crudo : new String[] {null, "", "   "}) {
                ScopesConcedidos concedidos = cliente.concederScopes(SolicitudScopes.desde(crudo));
                assertThat(concedidos.comoClaim()).contains("a b c");
            }
        }

        @Test
        @DisplayName("Debe conceder solo el subconjunto pedido, en el orden del archivo y sin duplicados")
        void concedeSubconjunto() {
            ClienteRegistrado cliente = cliente("api", List.of("a", "b", "c"), List.of(Audiencia.RABBITMQ));

            assertThat(cliente.concederScopes(SolicitudScopes.desde("c a")).comoClaim()).contains("a c");
            assertThat(cliente.concederScopes(SolicitudScopes.desde("  b   b  ")).comoClaim()).contains("b");
        }

        @Test
        @DisplayName("Los scopes deben distinguir mayúsculas")
        void distingueMayusculas() {
            ClienteRegistrado cliente = cliente("api", List.of("a"), List.of(Audiencia.RABBITMQ));

            assertThrows(ScopeNoPermitidoException.class, () -> cliente.concederScopes(SolicitudScopes.desde("A")));
        }

        @Test
        @DisplayName("Un scope válido junto a uno no asignado debe dar invalid_scope sin recortar")
        void mezclaNoRecorta() {
            ClienteRegistrado cliente = cliente("api", List.of("a", "b"), List.of(Audiencia.RABBITMQ));

            ScopeNoPermitidoException error = assertThrows(ScopeNoPermitidoException.class,
                    () -> cliente.concederScopes(SolicitudScopes.desde("a z")));
            assertThat(error.codigo()).isEqualTo(CodigoErrorOAuth.INVALID_SCOPE);
        }

        @Test
        @DisplayName("Un cliente sin scopes que pide un scope debe dar invalid_scope")
        void clienteSinScopesPide() {
            ClienteRegistrado cliente = cliente("voter_registration_service", List.of(), List.of(Audiencia.KAFKA));

            assertThrows(ScopeNoPermitidoException.class, () -> cliente.concederScopes(SolicitudScopes.desde("a")));
            assertThat(cliente.concederScopes(SolicitudScopes.desde(" ")).comoClaim()).isEmpty();
        }
    }

    @Nested
    @DisplayName("SolicitudScopes")
    class SolicitudScopesTest {

        @Test
        @DisplayName("Un scope que no cumple la gramática debe dar invalid_scope")
        void gramaticaInvalida() {
            ScopeNoPermitidoException error = assertThrows(ScopeNoPermitidoException.class,
                    () -> SolicitudScopes.desde("a\"b"));
            assertThat(error.codigo()).isEqualTo(CodigoErrorOAuth.INVALID_SCOPE);
        }

        @Test
        @DisplayName("Debe conservar : y / del scope")
        void conservaCaracteres() {
            ClienteRegistrado cliente = cliente("api", List.of("rabbitmq.read:votos/cola"), List.of(Audiencia.RABBITMQ));

            assertThat(cliente.concederScopes(SolicitudScopes.desde("rabbitmq.read:votos/cola")).comoClaim())
                    .contains("rabbitmq.read:votos/cola");
        }
    }

    @Nested
    @DisplayName("RegistroClientes")
    class RegistroClientesTest {

        @Test
        @DisplayName("Debe buscar por client_id y exponer el coste común")
        void buscaYCoste() {
            RegistroClientes registro = new RegistroClientes(List.of(
                    cliente("voting_api", List.of("a"), List.of(Audiencia.KAFKA, Audiencia.RABBITMQ)),
                    cliente("analytics_archiver", List.of(), List.of(Audiencia.KAFKA))));

            assertThat(registro.buscar(new ClientId("voting_api"))).isPresent();
            assertThat(registro.buscar(new ClientId("VOTING_API"))).isEmpty();
            assertThat(registro.costeComun()).isEqualTo(12);
        }

        @Test
        @DisplayName("Debe rechazar un registro vacío, client_id repetidos o costes distintos")
        void rechazaInvalidos() {
            assertThrows(IllegalArgumentException.class, () -> new RegistroClientes(List.of()));
            assertThrows(IllegalArgumentException.class, () -> new RegistroClientes(List.of(
                    cliente("a", List.of("x"), List.of(Audiencia.RABBITMQ)),
                    cliente("a", List.of("y"), List.of(Audiencia.RABBITMQ)))));
            assertThrows(IllegalArgumentException.class, () -> new RegistroClientes(List.of(
                    cliente("a", List.of("x"), List.of(Audiencia.RABBITMQ)),
                    new ClienteRegistrado(new ClientId("b"), HASH_10, List.of(new Scope("x")), List.of(Audiencia.RABBITMQ)))));
        }
    }

    @Nested
    @DisplayName("ClaimsToken")
    class ClaimsTokenTest {

        @Test
        @DisplayName("Debe calcular exp = iat + TTL con iat truncado a segundos")
        void calculaExp() {
            // Arrange
            ClienteRegistrado cliente = cliente("api", List.of("a", "b"), List.of(Audiencia.RABBITMQ, Audiencia.KAFKA));
            Instant ahora = Instant.parse("2026-11-14T10:00:00.750Z");

            // Act
            ClaimsToken claims = ClaimsToken.para(new Emisor("https://vm-auth"), cliente,
                    cliente.concederScopes(SolicitudScopes.desde(null)), ahora, Ttl.porDefecto());

            // Assert
            assertThat(claims.iss()).isEqualTo("https://vm-auth");
            assertThat(claims.sub()).isEqualTo("api");
            assertThat(claims.aud()).containsExactly(Audiencia.RABBITMQ, Audiencia.KAFKA);
            assertThat(claims.scope()).contains("a b");
            assertThat(claims.iat()).isEqualTo(ahora.getEpochSecond());
            assertThat(claims.exp()).isEqualTo(ahora.getEpochSecond() + 300);
        }

        @Test
        @DisplayName("Un cliente sin scopes no debe llevar claim scope")
        void sinScope() {
            ClienteRegistrado cliente = cliente("analytics_archiver", List.of(), List.of(Audiencia.KAFKA));

            ClaimsToken claims = ClaimsToken.para(new Emisor("https://vm-auth"), cliente,
                    cliente.concederScopes(SolicitudScopes.desde(null)), Instant.EPOCH, Ttl.porDefecto());

            assertThat(claims.scope()).isEmpty();
        }
    }

    @Nested
    @DisplayName("TokenEmitido")
    class TokenEmitidoTest {

        @Test
        @DisplayName("Debe enmascarar el token en toString")
        void enmascara() {
            assertThat(new TokenEmitido("eyJ.secreto.firma", 300).toString()).doesNotContain("secreto");
        }
    }
}
