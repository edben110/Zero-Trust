package com.zerotrust.voterregistration.port;

import com.zerotrust.voterregistration.domain.RegistroVotanteCrudo;

import java.util.List;

public interface FuentePadron {

    List<RegistroVotanteCrudo> leer();
}
