package com.zerotrust.voterregistration.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

// Todo o nada: con un solo registro inválido no se guarda ni se publica ningún votante.
public final class PadronInicial {

    private final List<VotanteRegistrado> votantes;

    private PadronInicial(List<VotanteRegistrado> votantes) {
        this.votantes = List.copyOf(votantes);
    }

    public static PadronInicial desde(List<RegistroVotanteCrudo> crudos, Instant registradoEn) {
        if (crudos.isEmpty()) {
            throw new PadronInvalidoException("el padrón está vacío");
        }
        List<VotanteRegistrado> votantes = new ArrayList<>();
        Set<VoterId> vistos = new HashSet<>();
        for (int i = 0; i < crudos.size(); i++) {
            RegistroVotanteCrudo crudo = crudos.get(i);
            String posicion = "registro [" + i + "]";
            VoterId voterId = convertir(crudo.voterId(), VoterId::new, posicion, "voterId");
            NombreCompleto nombre = convertir(crudo.fullName(), NombreCompleto::new, posicion, "fullName");
            Region region = convertir(crudo.region(), Region::new, posicion, "region");
            if (!vistos.add(voterId)) {
                throw new PadronInvalidoException(posicion + ": voterId repetido");
            }
            votantes.add(new VotanteRegistrado(voterId, nombre, region, registradoEn));
        }
        return new PadronInicial(votantes);
    }

    private static <T> T convertir(String valor, Function<String, T> conversor, String posicion, String campo) {
        try {
            return conversor.apply(valor);
        } catch (IllegalArgumentException e) {
            throw new PadronInvalidoException(posicion + ": " + campo + " inválido");
        }
    }

    public List<VotanteRegistrado> votantes() {
        return votantes;
    }
}
