package com.zerotrust.authserver.config;

import com.zerotrust.authserver.domain.Emisor;
import com.zerotrust.authserver.domain.IdClave;
import com.zerotrust.authserver.domain.PuertoServidor;
import com.zerotrust.authserver.domain.RegistroClientes;
import com.zerotrust.authserver.domain.Ttl;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public record ConfiguracionAuthServer(RegistroClientes registro, Emisor emisor, Ttl ttl, IdClave idClave,
                                      RSAPrivateKey clavePrivada, RSAPublicKey clavePublica, PuertoServidor puerto) {
}
