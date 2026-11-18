package com.zerotrust.voterregistration.adapter.persistence;

import com.zerotrust.voterregistration.domain.Region;
import com.zerotrust.voterregistration.domain.VotanteRegistrado;
import com.zerotrust.voterregistration.domain.VoterId;

public final class MapperVotante {

    private MapperVotante() {
    }

    public static VotanteRegistradoEntity aEntidad(VotanteRegistrado votante) {
        return new VotanteRegistradoEntity(votante.voterId().valor(),
                votante.region().valor(), votante.registradoEn());
    }

    // Reconstruye el dominio pasando por sus Value Objects, así un dato corrupto en la tabla no se cuela.
    public static VotanteRegistrado aDominio(VotanteRegistradoEntity entidad) {
        return new VotanteRegistrado(new VoterId(entidad.getVoterId()),
                new Region(entidad.getRegion()), entidad.getRegisteredAt());
    }
}
