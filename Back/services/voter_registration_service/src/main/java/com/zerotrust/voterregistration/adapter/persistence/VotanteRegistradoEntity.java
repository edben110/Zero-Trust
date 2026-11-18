package com.zerotrust.voterregistration.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// Entidad JPA inmutable de registered_voters (docs/schema.sql): sin setters, se crea con el constructor completo.
@Entity
@Table(name = "registered_voters")
public class VotanteRegistradoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voter_id", nullable = false, unique = true, length = 36, updatable = false)
    private String voterId;

    @Column(name = "region", nullable = false, length = 100, updatable = false)
    private String region;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    protected VotanteRegistradoEntity() {
        // exigido por JPA
    }

    public VotanteRegistradoEntity(String voterId, String region, Instant registeredAt) {
        this.voterId = voterId;
        this.region = region;
        this.registeredAt = registeredAt;
    }

    public String getVoterId() {
        return voterId;
    }

    String getRegion() {
        return region;
    }

    Instant getRegisteredAt() {
        return registeredAt;
    }
}
