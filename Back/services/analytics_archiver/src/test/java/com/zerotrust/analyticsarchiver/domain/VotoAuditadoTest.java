package com.zerotrust.analyticsarchiver.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VotoAuditadoTest {

    static final String VOTE = "11111111-2222-4333-8444-555555555555";
    static final String VOTANTE = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    static final String CANDIDATO = "0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8";

    @Test
    @DisplayName("Debe construir un voto auditado con identificadores normalizados")
    void construye() {
        // Act
        VotoAuditado voto = new VotoAuditado(new VoteId(VOTE.toUpperCase()), new VoterId(VOTANTE),
                new CandidateId(CANDIDATO), new Region("norte"), new SourceIp("203.0.113.7"),
                Instant.parse("2026-11-14T15:00:00Z"));

        // Assert
        assertThat(voto.voteId().valor()).isEqualTo(VOTE);
        assertThat(voto.votadoEn()).isEqualTo(Instant.parse("2026-11-14T15:00:00Z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "x", "1-1-1-1-1"})
    @DisplayName("Los identificadores deben ser UUID canónicos")
    void identificadores(String valor) {
        assertThrows(IllegalArgumentException.class, () -> new VoteId(valor));
        assertThrows(IllegalArgumentException.class, () -> new VoterId(valor));
        assertThrows(IllegalArgumentException.class, () -> new CandidateId(valor));
    }

    @Test
    @DisplayName("Region y SourceIp respetan el largo de vote_audit")
    void largos() {
        assertThrows(IllegalArgumentException.class, () -> new Region(" "));
        assertThrows(IllegalArgumentException.class, () -> new Region("r".repeat(101)));
        assertThrows(IllegalArgumentException.class, () -> new SourceIp(""));
        assertThrows(IllegalArgumentException.class, () -> new SourceIp("1".repeat(46)));
    }

    @Test
    @DisplayName("Ningún campo puede ser nulo")
    void nulos() {
        assertThrows(NullPointerException.class, () -> new VotoAuditado(new VoteId(VOTE), new VoterId(VOTANTE),
                new CandidateId(CANDIDATO), new Region("norte"), new SourceIp("1.1.1.1"), null));
        assertThrows(IllegalArgumentException.class, () -> new VoteId(null));
    }
}
