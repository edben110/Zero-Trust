package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.VotoEmitido;

public interface RegistroVotoAceptado {
    void registrar(VotoEmitido voto);
}
