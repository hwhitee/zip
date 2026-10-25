package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wardsentinel.backend.domain.model.RiskLevel;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InferenceResponseDto {

    @JsonProperty("result_id")
    private UUID resultId;

    @JsonProperty("window_id")
    private UUID windowId;

    @JsonProperty("patient_id")
    private UUID patientId;

    @JsonProperty("computed_at_ms")
    private Long computedAtMs;

    @JsonProperty("latency_ms")
    private Integer latencyMs;

    @JsonProperty("risk_score")
    private Double riskScore;

    @JsonProperty("risk_level")
    private RiskLevel riskLevel;

    @JsonProperty("contributing_factors")
    private List<ContributingFactorDto> contributingFactors;

    @JsonProperty("uncertainty")
    private Double uncertainty;

    @JsonProperty("missing_modalities")
    private List<String> missingModalities;

    @JsonProperty("warnings")
    private List<String> warnings;

    @JsonProperty("scenario_hint")
    private String scenarioHint;

    @JsonProperty("gemini_explanation")
    private String geminiExplanation;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("model_type")
    private String modelType;

    public InferenceResponseDto() {}

    public boolean isValid() {
        return resultId != null
                && windowId != null
                && riskScore != null
                && riskScore >= 0.0 && riskScore <= 1.0
                && riskLevel != null
                && modelVersion != null;
    }

    public UUID getResultId() { return resultId; }
    public void setResultId(UUID resultId) { this.resultId = resultId; }

    public UUID getWindowId() { return windowId; }
    public void setWindowId(UUID windowId) { this.windowId = windowId; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public Long getComputedAtMs() { return computedAtMs; }
    public void setComputedAtMs(Long computedAtMs) { this.computedAtMs = computedAtMs; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public List<ContributingFactorDto> getContributingFactors() { return contributingFactors; }
    public void setContributingFactors(List<ContributingFactorDto> contributingFactors) { this.contributingFactors = contributingFactors; }

    public Double getUncertainty() { return uncertainty; }
    public void setUncertainty(Double uncertainty) { this.uncertainty = uncertainty; }

    public List<String> getMissingModalities() { return missingModalities; }
    public void setMissingModalities(List<String> missingModalities) { this.missingModalities = missingModalities; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public String getScenarioHint() { return scenarioHint; }
    public void setScenarioHint(String scenarioHint) { this.scenarioHint = scenarioHint; }

    public String getGeminiExplanation() { return geminiExplanation; }
    public void setGeminiExplanation(String geminiExplanation) { this.geminiExplanation = geminiExplanation; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
}
