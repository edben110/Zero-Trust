package com.zerotrust.authserver.port;

import com.zerotrust.authserver.domain.HashSecreto;
import com.zerotrust.authserver.domain.SecretoCliente;

public interface VerificadorSecretos {

    boolean coincide(SecretoCliente secreto, HashSecreto hash);

    HashSecreto hashFicticio();
}
