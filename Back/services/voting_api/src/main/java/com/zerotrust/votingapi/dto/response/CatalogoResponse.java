package com.zerotrust.votingapi.dto.response;

import java.util.List;

public record CatalogoResponse(List<CandidatoResponse> candidates, List<String> regions) {

    public record CandidatoResponse(String candidateId, String name, String list) {
    }
}
