package com.zerotrust.analyticsarchiver.mapper;

import com.zerotrust.analyticsarchiver.repository.entity.VotoAuditadoEntity;
import com.zerotrust.analyticsarchiver.domain.CandidateId;
import com.zerotrust.analyticsarchiver.domain.Region;
import com.zerotrust.analyticsarchiver.domain.SourceIp;
import com.zerotrust.analyticsarchiver.domain.VoteId;
import com.zerotrust.analyticsarchiver.domain.VoterId;
import com.zerotrust.analyticsarchiver.domain.VotoAuditado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MapperVotoAuditado")
class MapperVotoAuditadoTest {

    private static final String VOTE = "11111111-2222-4333-8444-555555555555";
    private static final VotoAuditado VOTO = new VotoAuditado(new VoteId(VOTE),
            new VoterId("3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b"), new CandidateId("0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8"),
            new Region("norte"), new SourceIp("203.0.113.7"), Instant.parse("2026-11-14T15:00:00Z"));

    @Test
    @DisplayName("El mapper convierte en ambos sentidos sin perder datos")
    void mapper() {
        VotoAuditadoEntity entidad = MapperVotoAuditado.aEntidad(VOTO);

        assertThat(entidad.getVoteId()).isEqualTo(VOTE);
        assertThat(MapperVotoAuditado.aDominio(entidad)).isEqualTo(VOTO);
    }
}
