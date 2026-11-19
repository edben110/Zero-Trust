package com.zerotrust.botdetector.repository.entity;

import com.zerotrust.botdetector.domain.AlertOrigin;
import com.zerotrust.botdetector.domain.FraudKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// Append-only: solo la purga explícita del panel vacía la tabla.
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FraudKind kind;

    @Column(nullable = false)
    private Instant detectedAt;

    protected FraudIncidentEntity() {}

    public FraudIncidentEntity(String ip, String riskLevel, int voteCount,
                                int windowSeconds, AlertOrigin origin, FraudKind kind, Instant detectedAt) {
        this.ip            = ip;
        this.riskLevel     = riskLevel;
        this.voteCount     = voteCount;
        this.windowSeconds = windowSeconds;
        this.origin        = origin;
        this.kind          = kind;
        this.detectedAt    = detectedAt;
    }

    public Long        getId()            { return id; }
    public String      getIp()            { return ip; }
    public String      getRiskLevel()     { return riskLevel; }
    public int         getVoteCount()     { return voteCount; }
    public int         getWindowSeconds() { return windowSeconds; }
    public AlertOrigin getOrigin()        { return origin; }
    public FraudKind   getKind()          { return kind; }
    public Instant     getDetectedAt()    { return detectedAt; }
}
