package com.zerotrust.authserver.service;

import com.zerotrust.authserver.domain.Audiencia;
import com.zerotrust.authserver.domain.ClaimsToken;
import com.zerotrust.authserver.domain.ClientId;
import com.zerotrust.authserver.domain.ClienteInvalidoException;
import com.zerotrust.authserver.domain.ClienteRegistrado;
import com.zerotrust.authserver.domain.Emisor;
import com.zerotrust.authserver.domain.GrantNoSoportadoException;
import com.zerotrust.authserver.domain.HashSecreto;
import com.zerotrust.authserver.domain.RegistroClientes;
import com.zerotrust.authserver.domain.Scope;
import com.zerotrust.authserver.domain.ScopeNoPermitidoException;
import com.zerotrust.authserver.domain.SecretoCliente;
import com.zerotrust.authserver.domain.TokenEmitido;
import com.zerotrust.authserver.domain.Ttl;
import com.zerotrust.authserver.port.FirmadorTokens;
import com.zerotrust.authserver.port.VerificadorSecretos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioEmisionTokenTest {

    private static final HashSecreto HASH_REAL = new HashSecreto("$2b$12$" + "r".repeat(53));
    private static final HashSecreto HASH_FICTICIO = new HashSecreto("$2b$12$" + "f".repeat(53));
    private static final Instant AHORA = Instant.parse("2026-11-14T12:00:00Z");
    private static final SecretoCliente SECRETO = new SecretoCliente("secreto-correcto");

    @Mock
    private VerificadorSecretos verificador;

    @Mock
    private FirmadorTokens firmador;

    private ServicioEmisionToken servicio;

    @BeforeEach
    void construirServicio() {
        RegistroClientes registro = new RegistroClientes(List.of(
                new ClienteRegistrado(new ClientId("voting_api"), HASH_REAL,
                        List.of(new Scope("a"), new Scope("b")), List.of(Audiencia.RABBITMQ, Audiencia.KAFKA))));
        servicio = new ServicioEmisionToken(registro, verificador, firmador, new Emisor("https://vm-auth"),
                Ttl.porDefecto(), Clock.fixed(AHORA, ZoneOffset.UTC));
        lenient().when(verificador.hashFicticio()).thenReturn(HASH_FICTICIO);
    }

    private static PeticionToken peticion(String grant, String clientId, SecretoCliente secreto, String scope) {
        return new PeticionToken(grant, clientId, secreto, scope, true);
    }

    @Test
    @DisplayName("Debe emitir el token con los claims del cliente y el TTL")
    void emiteToken() {
        // Arrange
        when(verificador.coincide(SECRETO, HASH_REAL)).thenReturn(true);
        when(firmador.firmar(any())).thenReturn("jwt-firmado");
        ArgumentCaptor<ClaimsToken> claims = ArgumentCaptor.forClass(ClaimsToken.class);

        // Act
        TokenEmitido token = servicio.emitir(peticion("client_credentials", "voting_api", SECRETO, null));

        // Assert
        assertThat(token.accessToken()).isEqualTo("jwt-firmado");
        assertThat(token.expiresIn()).isEqualTo(300);
        verify(firmador, times(1)).firmar(claims.capture());
        assertThat(claims.getValue().sub()).isEqualTo("voting_api");
        assertThat(claims.getValue().aud()).containsExactly(Audiencia.RABBITMQ, Audiencia.KAFKA);
        assertThat(claims.getValue().scope()).contains("a b");
        assertThat(claims.getValue().iat()).isEqualTo(AHORA.getEpochSecond());
        assertThat(claims.getValue().exp()).isEqualTo(AHORA.getEpochSecond() + 300);
    }

    @Test
    @DisplayName("Un grant distinto debe dar unsupported_grant_type antes de mirar las credenciales")
    void grantNoSoportadoPrimero() {
        GrantNoSoportadoException error = assertThrows(GrantNoSoportadoException.class,
                () -> servicio.emitir(peticion("password", "desconocido", new SecretoCliente("malo"), null)));

        assertThat(error.clientIdCrudo()).contains("desconocido");
        verifyNoInteractions(verificador, firmador);
    }

    @Test
    @DisplayName("El grant distingue mayúsculas")
    void grantDistingueMayusculas() {
        assertThrows(GrantNoSoportadoException.class,
                () -> servicio.emitir(peticion("Client_Credentials", "voting_api", SECRETO, null)));
    }

    @Test
    @DisplayName("Un secreto de más de 72 bytes debe dar invalid_client sin calcular BCrypt")
    void secretoLargoSinBcrypt() {
        assertThrows(ClienteInvalidoException.class,
                () -> servicio.emitir(peticion("client_credentials", "voting_api", new SecretoCliente("x".repeat(73)), null)));

        verify(verificador, never()).coincide(any(), any());
        verifyNoInteractions(firmador);
    }

    @Test
    @DisplayName("Un client_id inexistente debe verificarse contra el hash ficticio y dar invalid_client")
    void clienteInexistenteUsaHashFicticio() {
        ClienteInvalidoException error = assertThrows(ClienteInvalidoException.class,
                () -> servicio.emitir(peticion("client_credentials", "desconocido", SECRETO, null)));

        assertThat(error.porBasic()).isTrue();
        verify(verificador).hashFicticio();
        verify(verificador, times(1)).coincide(SECRETO, HASH_FICTICIO);
        verifyNoMoreInteractions(verificador);
        verifyNoInteractions(firmador);
    }

    @Test
    @DisplayName("Un client_id con formato inválido debe seguir el camino de cliente inexistente")
    void clienteConFormatoInvalido() {
        assertThrows(ClienteInvalidoException.class,
                () -> servicio.emitir(new PeticionToken("client_credentials", "a b", SECRETO, null, false)));

        verify(verificador, times(1)).coincide(SECRETO, HASH_FICTICIO);
    }

    @Test
    @DisplayName("Un secreto incorrecto de un cliente registrado debe dar invalid_client")
    void secretoIncorrecto() {
        SecretoCliente malo = new SecretoCliente("malo");
        when(verificador.coincide(malo, HASH_REAL)).thenReturn(false);

        ClienteInvalidoException error = assertThrows(ClienteInvalidoException.class,
                () -> servicio.emitir(new PeticionToken("client_credentials", "voting_api", malo, null, false)));

        assertThat(error.porBasic()).isFalse();
        assertThat(error.clientIdCrudo()).contains("voting_api");
        verify(verificador, never()).coincide(malo, HASH_FICTICIO);
        verifyNoInteractions(firmador);
    }

    @Test
    @DisplayName("Un scope mal formado con credenciales incorrectas debe dar invalid_client")
    void scopeMalFormadoConCredencialesMalas() {
        SecretoCliente malo = new SecretoCliente("malo");
        when(verificador.coincide(malo, HASH_REAL)).thenReturn(false);

        assertThrows(ClienteInvalidoException.class,
                () -> servicio.emitir(peticion("client_credentials", "voting_api", malo, "a\"b")));
    }

    @Test
    @DisplayName("Un scope mal formado o no asignado con credenciales correctas debe dar invalid_scope")
    void scopeConCredencialesBuenas() {
        when(verificador.coincide(SECRETO, HASH_REAL)).thenReturn(true);

        ScopeNoPermitidoException malFormado = assertThrows(ScopeNoPermitidoException.class,
                () -> servicio.emitir(peticion("client_credentials", "voting_api", SECRETO, "a\"b")));
        assertThrows(ScopeNoPermitidoException.class,
                () -> servicio.emitir(peticion("client_credentials", "voting_api", SECRETO, "a z")));

        assertThat(malFormado.clientIdCrudo()).contains("voting_api");
        verifyNoInteractions(firmador);
    }

    @Test
    @DisplayName("El orden debe ser grant, después cliente y después scope")
    void ordenCompleto() {
        when(verificador.coincide(SECRETO, HASH_REAL)).thenReturn(true);
        when(firmador.firmar(any())).thenReturn("jwt");

        servicio.emitir(peticion("client_credentials", "voting_api", SECRETO, "b"));

        InOrder orden = inOrder(verificador, firmador);
        orden.verify(verificador).coincide(SECRETO, HASH_REAL);
        orden.verify(firmador).firmar(any());
    }
}
