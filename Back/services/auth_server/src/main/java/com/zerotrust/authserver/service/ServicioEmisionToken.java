package com.zerotrust.authserver.service;

import com.zerotrust.authserver.domain.ClaimsToken;
import com.zerotrust.authserver.domain.ClientId;
import com.zerotrust.authserver.exception.ClienteInvalidoException;
import com.zerotrust.authserver.domain.ClienteRegistrado;
import com.zerotrust.authserver.domain.Emisor;
import com.zerotrust.authserver.exception.GrantNoSoportadoException;
import com.zerotrust.authserver.domain.RegistroClientes;
import com.zerotrust.authserver.exception.ScopeNoPermitidoException;
import com.zerotrust.authserver.domain.ScopesConcedidos;
import com.zerotrust.authserver.domain.SolicitudScopes;
import com.zerotrust.authserver.domain.TokenEmitido;
import com.zerotrust.authserver.domain.Ttl;
import com.zerotrust.authserver.port.FirmadorTokens;
import com.zerotrust.authserver.port.VerificadorSecretos;

import java.time.Clock;
import java.util.Optional;

public class ServicioEmisionToken {

    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";

    private final RegistroClientes registro;
    private final VerificadorSecretos verificador;
    private final FirmadorTokens firmador;
    private final Emisor emisor;
    private final Ttl ttl;
    private final Clock reloj;

    public ServicioEmisionToken(RegistroClientes registro, VerificadorSecretos verificador, FirmadorTokens firmador,
                                Emisor emisor, Ttl ttl, Clock reloj) {
        this.registro = registro;
        this.verificador = verificador;
        this.firmador = firmador;
        this.emisor = emisor;
        this.ttl = ttl;
        this.reloj = reloj;
    }

    public TokenEmitido emitir(PeticionToken peticion) {
        if (!GRANT_CLIENT_CREDENTIALS.equals(peticion.grantType())) {
            throw new GrantNoSoportadoException(peticion.clientIdCrudo());
        }
        ClienteRegistrado cliente = autenticar(peticion);
        ScopesConcedidos scopes = concederScopes(cliente, peticion);
        ClaimsToken claims = ClaimsToken.para(emisor, cliente, scopes, reloj.instant(), ttl);
        return new TokenEmitido(firmador.firmar(claims), ttl.segundos());
    }

    private ClienteRegistrado autenticar(PeticionToken peticion) {
        if (peticion.secreto().excedeLimiteBcrypt()) {
            throw clienteInvalido(peticion);
        }
        Optional<ClienteRegistrado> cliente = ClientId.intentar(peticion.clientIdCrudo()).flatMap(registro::buscar);
        if (cliente.isEmpty()) {
            // Mismo trabajo BCrypt que con un cliente real, para que el tiempo no revele qué client_id existen.
            verificador.coincide(peticion.secreto(), verificador.hashFicticio());
            throw clienteInvalido(peticion);
        }
        if (!verificador.coincide(peticion.secreto(), cliente.get().hash())) {
            throw clienteInvalido(peticion);
        }
        return cliente.get();
    }

    private static ScopesConcedidos concederScopes(ClienteRegistrado cliente, PeticionToken peticion) {
        try {
            return cliente.concederScopes(SolicitudScopes.desde(peticion.scopeCrudo()));
        } catch (ScopeNoPermitidoException e) {
            throw new ScopeNoPermitidoException(peticion.clientIdCrudo());
        }
    }

    private static ClienteInvalidoException clienteInvalido(PeticionToken peticion) {
        return new ClienteInvalidoException(peticion.porBasic(), peticion.clientIdCrudo());
    }
}
