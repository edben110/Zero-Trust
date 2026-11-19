package com.zerotrust.authserver.security;

import com.zerotrust.authserver.domain.HashSecreto;
import com.zerotrust.authserver.domain.SecretoCliente;
import com.zerotrust.authserver.port.VerificadorSecretos;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.security.SecureRandom;
import java.util.Base64;

public class VerificadorBcrypt implements VerificadorSecretos {

    private final HashSecreto hashFicticio;

    // Se calcula una sola vez: recalcularlo por petición haría más lento el camino de un cliente inexistente.
    public VerificadorBcrypt(int costeComun) {
        byte[] aleatorio = new byte[32];
        new SecureRandom().nextBytes(aleatorio);
        String secretoDescartado = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        this.hashFicticio = new HashSecreto(BCrypt.hashpw(secretoDescartado, BCrypt.gensalt(costeComun)));
    }

    @Override
    public boolean coincide(SecretoCliente secreto, HashSecreto hash) {
        return BCrypt.checkpw(secreto.valor(), hash.valor());
    }

    @Override
    public HashSecreto hashFicticio() {
        return hashFicticio;
    }
}
