package com.zerotrust.voterregistration.mapper;

import com.zerotrust.voterregistration.repository.entity.VotanteRegistradoEntity;
import com.zerotrust.voterregistration.domain.Region;
import com.zerotrust.voterregistration.domain.VotanteRegistrado;
import com.zerotrust.voterregistration.domain.VoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MapperVotante")
class MapperVotanteTest {

    private static final String ID_1 = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final Instant AHORA = Instant.parse("2026-11-14T08:00:00Z");

    private static VotanteRegistrado votante(String id) {
        return new VotanteRegistrado(new VoterId(id), new Region("norte"), AHORA);
    }

    @Test
    @DisplayName("Debe convertir del dominio a la entidad y de vuelta sin perder datos")
    void idaYVuelta() {
        VotanteRegistrado original = votante(ID_1);

        VotanteRegistradoEntity entidad = MapperVotante.aEntidad(original);

        assertThat(entidad.getVoterId()).isEqualTo(ID_1);
        assertThat(MapperVotante.aDominio(entidad)).isEqualTo(original);
    }
}
