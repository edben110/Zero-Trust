package com.zerotrust.voterregistration.port;

import com.zerotrust.voterregistration.domain.VotanteRegistrado;

public interface PublicadorElegibles {

    void publicar(VotanteRegistrado votante);
}
