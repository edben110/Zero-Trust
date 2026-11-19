package com.zerotrust.votingapi.domain;

import com.zerotrust.votingapi.exception.SolicitudVotoInvalidaException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public record SolicitudVoto(VoterId voterId, CandidateId candidateId, Region region) {

    public SolicitudVoto {
        Objects.requireNonNull(voterId, "voterId obligatorio");
        Objects.requireNonNull(candidateId, "candidateId obligatorio");
        Objects.requireNonNull(region, "region obligatoria");
    }

    // Reúne todos los campos inválidos para que el cliente pueda corregirlos de una vez.
    public static SolicitudVoto desde(String voterId, String candidateId, String region, Catalogo catalogo) {
        List<String> invalidos = new ArrayList<>();
        Optional<VoterId> votante = convertir(voterId, VoterId::new, v -> true, "voterId", invalidos);
        Optional<CandidateId> candidato = convertir(candidateId, CandidateId::new, catalogo::existeCandidato,
                "candidateId", invalidos);
        Optional<Region> laRegion = convertir(region, Region::new, catalogo::existeRegion, "region", invalidos);
        if (!invalidos.isEmpty()) {
            throw new SolicitudVotoInvalidaException(invalidos);
        }
        return new SolicitudVoto(votante.orElseThrow(), candidato.orElseThrow(), laRegion.orElseThrow());
    }

    private static <T> Optional<T> convertir(String crudo, Function<String, T> conversor, Predicate<T> enCatalogo,
                                             String campo, List<String> invalidos) {
        try {
            T valor = conversor.apply(crudo);
            if (enCatalogo.test(valor)) {
                return Optional.of(valor);
            }
        } catch (IllegalArgumentException e) {
            // el campo se marca como inválido abajo
        }
        invalidos.add(campo);
        return Optional.empty();
    }
}
