package com.zerotrust.analyticsarchiver.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "vote_audit")
public class VotoAuditadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vote_id", nullable = false, unique = true, length = 36, updatable = false)
    private String voteId;

    @Column(name = "voter_id", nullable = false, length = 36, updatable = false)
    private String voterId;

    @Column(name = "candidate_id", nullable = false, length = 36, updatable = false)
    private String candidateId;

    @Column(name = "region", nullable = false, length = 100, updatable = false)
    private String region;

    @Column(name = "source_ip", nullable = false, length = 45, updatable = false)
    private String sourceIp;

    @Column(name = "voted_at", nullable = false, updatable = false)
    private Instant votedAt;

    protected VotoAuditadoEntity() {
    }

    public VotoAuditadoEntity(String voteId, String voterId, String candidateId, String region, String sourceIp,
                              Instant votedAt) {
        this.voteId = voteId;
        this.voterId = voterId;
        this.candidateId = candidateId;
        this.region = region;
        this.sourceIp = sourceIp;
        this.votedAt = votedAt;
    }

    public String getVoteId() {
        return voteId;
    }

    public String getVoterId() {
        return voterId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public String getRegion() {
        return region;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public Instant getVotedAt() {
        return votedAt;
    }
}
