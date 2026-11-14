package com.zerotrust.votingapi.adapter.web;

import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.domain.Region;

import java.util.List;

public record CatalogoResponse(List<CandidatoResponse> candidates, List<String> regions) {

    public record CandidatoResponse(String candidateId, String name, String list) {
    }

    static CatalogoResponse de(Catalogo catalogo) {
        List<CandidatoResponse> candidatos = catalogo.candidatos().stream()
                .map(c -> new CandidatoResponse(c.candidateId().valor(), c.nombre(), c.lista()))
                .toList();
        return new CatalogoResponse(candidatos, catalogo.regiones().stream().map(Region::valor).toList());
    }
}
