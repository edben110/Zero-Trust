package com.zerotrust.botdetector.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad JPA de auditoría para incidentes de fraude.
 * Vive en la capa de adaptadores; el dominio nunca la ve directamente.
 *
 * La tabla es append-only: no se proporcionan métodos de actualización.
 */
@Entity
@Table(name = "fraud_incidents")
public class FraudIncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(nullable = false, length = 20)
    private String riskLevel;

    @Column(nullable = false)
    private int voteCount;

    @Column(nullable = false)
    private int windowSeconds;

    @Column(nullable = false)
    private Instant detectedAt;

    // Constructor JPA sin args
    protected FraudIncidentEntity() {}

    public FraudIncidentEntity(String ip, String riskLevel, int voteCount,
                                int windowSeconds, Instant detectedAt) {
        this.ip            = ip;
        this.riskLevel     = riskLevel;
        this.voteCount     = voteCount;
        this.windowSeconds = windowSeconds;
        this.detectedAt    = detectedAt;
    }

    // Accesores de solo lectura
    public Long    getId()           { return id; }
    public String  getIp()           { return ip; }
    public String  getRiskLevel()    { return riskLevel; }
    public int     getVoteCount()    { return voteCount; }
    public int     getWindowSeconds(){ return windowSeconds; }
    public Instant getDetectedAt()   { return detectedAt; }
}
