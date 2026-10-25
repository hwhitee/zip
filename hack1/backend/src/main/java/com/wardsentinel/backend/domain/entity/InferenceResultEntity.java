package com.wardsentinel.backend.domain.entity;

import com.wardsentinel.backend.domain.model.RiskLevel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inference_results")
public class InferenceResultEntity {

    @Id
    @Column(name = "result_id")
    private UUID resultId;

    @Column(name = "window_id", nullable = false)
    private UUID windowId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "risk_score")
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel;

    @Column(name = "contributing_factors", columnDefinition = "TEXT")
    private String contributingFactors;

    @Column(name = "gemini_explanation", columnDefinition = "TEXT")
    private String geminiExplanation;

    @Column(name = "model_version", nullable = false, length = 32)
    private String modelVersion;

    @Column(name = "model_type", nullable = false, length = 16)
    private String modelType = "HEURISTIC";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public InferenceResultEntity() {}

    public InferenceResultEntity(UUID resultId, UUID windowId, UUID patientId, Instant computedAt, Integer latencyMs, Double riskScore, RiskLevel riskLevel, String contributingFactors, String geminiExplanation, String modelVersion, String modelType) {
        this.resultId = resultId != null ? resultId : UUID.randomUUID();
        this.windowId = windowId;
        this.patientId = patientId;
        this.computedAt = computedAt != null ? computedAt : Instant.now();
        this.latencyMs = latencyMs;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.contributingFactors = contributingFactors;
        this.geminiExplanation = geminiExplanation;
        this.modelVersion = modelVersion;
        this.modelType = modelType != null ? modelType : "HEURISTIC";
        this.createdAt = Instant.now();
    }

    public UUID getResultId() { return resultId; }
    public void setResultId(UUID resultId) { this.resultId = resultId; }

    public UUID getWindowId() { return windowId; }
    public void setWindowId(UUID windowId) { this.windowId = windowId; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getContributingFactors() { return contributingFactors; }
    public void setContributingFactors(String contributingFactors) { this.contributingFactors = contributingFactors; }

    public String getGeminiExplanation() { return geminiExplanation; }
    public void setGeminiExplanation(String geminiExplanation) { this.geminiExplanation = geminiExplanation; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
