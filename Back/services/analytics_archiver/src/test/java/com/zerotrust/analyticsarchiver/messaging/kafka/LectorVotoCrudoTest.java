package com.zerotrust.analyticsarchiver.messaging.kafka;

import com.zerotrust.analyticsarchiver.domain.CandidateId;
import com.zerotrust.analyticsarchiver.domain.Region;
import com.zerotrust.analyticsarchiver.domain.SourceIp;
import com.zerotrust.analyticsarchiver.domain.VoteId;
import com.zerotrust.analyticsarchiver.domain.VoterId;
import com.zerotrust.analyticsarchiver.domain.VotoAuditado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LectorVotoCrudo")
class LectorVotoCrudoTest {

    private static final String VOTE = "11111111-2222-4333-8444-555555555555";
    private static final String PAYLOAD = "{\"voteId\":\"" + VOTE + "\",\"voterId\":\"3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b\","
            + "\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\",\"region\":\"norte\","
            + "\"sourceIp\":\"203.0.113.7\",\"timestamp\":\"2026-11-14T15:00:00Z\"}";
    private static final VotoAuditado VOTO = new VotoAuditado(new VoteId(VOTE),
            new VoterId("3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b"), new CandidateId("0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8"),
            new Region("norte"), new SourceIp("203.0.113.7"), Instant.parse("2026-11-14T15:00:00Z"));

    private final LectorVotoCrudo lector = new LectorVotoCrudo();

    @Test
    @DisplayName("Debe leer el payload de raw_votes")
    void lee() {
        assertThat(lector.interpretar(VOTE, PAYLOAD)).contains(VOTO);
        assertThat(lector.interpretar(null, PAYLOAD)).contains(VOTO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no es json", "{}", "[]",
            "{\"voteId\":\"x\",\"voterId\":\"3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b\",\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\",\"region\":\"norte\",\"sourceIp\":\"1.1.1.1\",\"timestamp\":\"2026-11-14T15:00:00Z\"}",
            "{\"voteId\":\"11111111-2222-4333-8444-555555555555\",\"voterId\":\"3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b\",\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\",\"region\":\"norte\",\"sourceIp\":\"1.1.1.1\",\"timestamp\":\"ayer\"}",
            "{\"voteId\":\"11111111-2222-4333-8444-555555555555\",\"voterId\":\"3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b\",\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\",\"sourceIp\":\"1.1.1.1\",\"timestamp\":\"2026-11-14T15:00:00Z\"}"})
    @DisplayName("Un mensaje malformado se descarta")
    void malformado(String valor) {
        assertThat(lector.interpretar(VOTE, valor)).isEmpty();
        assertThat(lector.interpretar(VOTE, null)).isEmpty();
    }

    @Test
    @DisplayName("Una key distinta del voteId se descarta")
    void keyDistinta() {
        assertThat(lector.interpretar("9a8b7c6d-5e4f-4a3b-8c2d-1e0f2a3b4c5d", PAYLOAD)).isEmpty();
    }
}
