package com.zerotrust.votingapi.mapper;

import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.dto.response.CatalogoResponse;
import com.zerotrust.votingapi.dto.response.CatalogoResponse.CandidatoResponse;

import java.util.List;

public final class MapperCatalogo {

    private MapperCatalogo() {
    }

    public static CatalogoResponse aResponse(Catalogo catalogo) {
        List<CandidatoResponse> candidatos = catalogo.candidatos().stream()
                .map(c -> new CandidatoResponse(c.candidateId().valor(), c.nombre(), c.lista()))
                .toList();
        return new CatalogoResponse(candidatos, catalogo.regiones().stream().map(Region::valor).toList());
    }
}
